package proxy.forex.entity

import proxy.forex.model.CurrencyPair
import play.api.libs.json.{Format, Json}

case class RatesRequest(rates: Seq[CurrencyPair])

object RatesRequest {
  implicit val ratesRequestFormatter: Format[RatesRequest] = Json.format[RatesRequest]
}