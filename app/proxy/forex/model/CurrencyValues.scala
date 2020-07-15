package proxy.forex.model

import play.api.libs.json._
import proxy.forex.exception.AppError

import scala.util.{Failure, Success, Try}

object CurrencyValues {

  sealed trait Currency extends Product with Serializable

  case object AUD extends Currency

  case object CAD extends Currency

  case object CHF extends Currency

  case object EUR extends Currency

  case object GBP extends Currency

  case object NZD extends Currency

  case object JPY extends Currency

  case object SGD extends Currency

  case object USD extends Currency

  case object UnSupportedCurrency extends Currency

  val all = Seq(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)

  implicit val currencyFormat: Format[Currency] = format(all.toSet)

  def fromString(str: String): Try[Currency] = all.find(_.toString == str) match {
    case Some(s) => Success(s)
    case None => Failure(CurrencyUnsupported(str))
  }

  def format[T](all: Set[T]): Format[T] = new Format[T] {
    override def writes(o: T): JsValue = Json.toJson(o.toString)

    override def reads(json: JsValue): JsResult[T] = json match {
      case JsString(name) => all.find(_.toString == name).map(JsSuccess(_)).getOrElse(JsError(s"Cant find enum value matching input $name"))
      case _ => JsError(s"Cant find enum value matching input $json")
    }
  }

  case class CurrencyUnsupported(currency: String) extends AppError {

    override def message: String = s"Currency $currency is not supported or invalid"
  }
}