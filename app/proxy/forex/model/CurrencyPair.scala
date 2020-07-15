package proxy.forex.model

import proxy.forex.model.CurrencyValues.Currency
import play.api.libs.json.{Format, Json}

case class CurrencyPair(from: Currency, to: Currency)

object CurrencyPair {
  implicit val ratePairFormatter: Format[CurrencyPair] = Json.format[CurrencyPair]
}
