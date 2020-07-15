package proxy.forex.services

import akka.Done
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.cache.AsyncCacheApi
import proxy.forex.client.OneFrameClient
import proxy.forex.entity.{OneFrameRate, RatesResponse}
import proxy.forex.exception.AppError
import proxy.forex.logging.PlayLogging
import proxy.forex.model.CurrencyPair
import proxy.forex.model.CurrencyValues.Currency
import proxy.forex.services.RatesService.{CacheIsNotReady, CacheUpdateError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[RatesServiceImpl])
trait RatesService {

  def convert(currencyPairs: Seq[CurrencyPair]): Future[Seq[RatesResponse]]

  def update(currencies: Seq[Currency]): Future[Done]

}

@Singleton
class RatesServiceImpl @Inject()(oneFrameClient: OneFrameClient, asyncCacheApi: AsyncCacheApi)(implicit val ec: ExecutionContext) extends RatesService with PlayLogging {

  private val RatesKey = "rates"

  override def convert(currencyPairs: Seq[CurrencyPair]): Future[Seq[RatesResponse]] = {
    (for {
      rates <- asyncCacheApi.get[Seq[OneFrameRate]](RatesKey).flatMap(failsIfEmpty)
      pair = rates.filter(frameRate => currencyPairs.exists(currency => frameRate.from == currency.from && currency.to == frameRate.to))
    } yield pair).map(RatesResponse(_))
  }

  override def update(currencies: Seq[Currency]): Future[Done] = {
    logger.info("Updating currencies in the cache: " + currencies)
    val pairs = currencies.combinations(2).flatMap(_.permutations).toList.map(currency => CurrencyPair(currency.head, currency.last))
    (for {
      _ <- asyncCacheApi.remove(RatesKey)
      freshRates <- oneFrameClient.fetch(pairs)
      result <- asyncCacheApi.set(RatesKey, freshRates)
    } yield result).recoverWith {
      case NonFatal(ex) =>
        val error = CacheUpdateError(ex)
        logger.error(error.toString);
        Future.failed(error)
    }
  }

  private def failsIfEmpty(data: Option[Seq[OneFrameRate]]): Future[Seq[OneFrameRate]] = data match {
    case Some(result) if result.nonEmpty => Future.successful(result)
    case _ => Future.failed(CacheIsNotReady())
  }
}

object RatesService {

  case class CacheUpdateError(ex: Throwable) extends AppError {
    override def message: String = s"Unable to update cache. Exception: $ex"
  }

  case class CacheIsNotReady() extends AppError {
    override def message: String = "API is not ready. Please wait and retry"
  }

}
