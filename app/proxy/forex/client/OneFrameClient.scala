package proxy.forex.client

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.libs.ws.{WSClient, WSResponse}
import proxy.forex.client.OneFrameClient.{OneFrameClientError, UnsupportedOperation}
import proxy.forex.entity.OneFrameRate
import proxy.forex.exception.AppError
import proxy.forex.logging.PlayLogging
import proxy.forex.model.CurrencyPair

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[OneFrameClientImpl])
trait OneFrameClient {

  def fetch(ratePairs: Seq[CurrencyPair]): Future[Seq[OneFrameRate]]

}

@Singleton
class OneFrameClientImpl @Inject()(wsClient: WSClient, configuration: Configuration)(implicit val ec: ExecutionContext) extends OneFrameClient with PlayLogging {

  private val Url: String = configuration.get[String]("one-frame.url")
  private val Token: String = configuration.get[String]("one-frame.token")

  override def fetch(ratePairs: Seq[CurrencyPair]): Future[Seq[OneFrameRate]] = {
    val url = ratePairs.foldLeft(s"$Url?") { (acc, currency) =>
      acc.concat(s"pair=${currency.from}${currency.to}&")
    }
    val wsResponse: Future[WSResponse] = wsClient.url(url).withHttpHeaders("token" -> Token).get()
    parseResponse[Seq[OneFrameRate]](wsResponse)
  }

  private def parseResponse[T: Reads](response: Future[WSResponse]): Future[T] = response.transform {
    case Success(result) if result.status == Status.OK => parseResponseBody(result)
    case Success(_) => Failure(UnsupportedOperation())
    case Failure(ex) => Failure(OneFrameClientError(ex))
  }

  private def parseResponseBody[T: Reads](response: WSResponse): Try[T] = Json.fromJson[T](response.json) match {
    case JsSuccess(parsed, _) => Success(parsed)
    case JsError(error) => Failure(new RuntimeException(s"Failed to parse json ${response.body} response with error: $error"))
  }
}

object OneFrameClient {

  case class UnsupportedOperation() extends AppError {
    override def message: String = "Status of operation is not satisfied"
  }

  case class OneFrameClientError(ex: Throwable) extends AppError {
    override def message: String = s"Unable to reach service. Exception is: $ex"
  }

}