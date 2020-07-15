package validators

import javax.inject.{Inject, Singleton}
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import proxy.forex.exception.AppError
import proxy.forex.model.{CurrencyPair, CurrencyValues}
import validators.CurrencyExchangeRequestValidator.RequestValidationError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class CurrencyExchangeRequestValidator @Inject()()(implicit val ec: ExecutionContext) {

  def validateToRun[T](body: Seq[CurrencyPair] => Future[Result])(implicit request: Request[T]): Future[Result] = {
    val currencyPairs = request.queryString.get("pair")
    val currencyPairsOption = currencyPairs.map(t => t.map(_.splitAt(3)))
    currencyPairsOption match {
      case Some(s) if s.nonEmpty => handleResponse(s).flatMap(body(_))
      case Some(_) => Future.successful(BadRequest(RequestValidationError("Rates cannot be empty").message))
      case None => Future.successful(BadRequest(RequestValidationError("Rates cannot be empty").message))
    }
  }

  private def handleResponse(currencies: Seq[(String, String)]): Future[Seq[CurrencyPair]] = validateRates(currencies) match {
    case (Nil, Nil) => Future.failed(RequestValidationError("No valid currencies in the request"))
    case (Nil, currencies) => Future.successful(currencies)
    case (errors, _) => Future.failed(errors.head)
  }

  private def validateRates(currencies: Seq[(String, String)]): (Seq[Throwable], Seq[CurrencyPair]) = currencies.foldLeft(Seq[Throwable](), Seq[CurrencyPair]()) { (acc, currencyPairTuple) => {
    val (errors, currencyPairs) = acc
    val (from, to) = currencyPairTuple
    (CurrencyValues.fromString(from), CurrencyValues.fromString(to)) match {
      case (Success(value1), Success(value2)) => (errors, currencyPairs :+ CurrencyPair(value1, value2))
      case (Failure(ex), Failure(ex2)) => (errors :+ ex :+ ex2, currencyPairs)
      case (_, Failure(ex)) => (errors :+ ex, currencyPairs)
      case (Failure(ex), _) => (errors :+ ex, currencyPairs)
    }
  }
  }
}

object CurrencyExchangeRequestValidator {

  case class WrongCurrencyCode(code: String)

  case class RequestValidationError(reason: String) extends AppError {
    override def message: String = reason
  }

}