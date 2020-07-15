package proxy.forex.entity

import java.time.LocalDateTime

import proxy.forex.model.CurrencyValues.Currency
import play.api.libs.json.{Format, Json}

case class OneFrameRate(from: Currency, to: Currency, bid: BigDecimal, ask: BigDecimal, price: BigDecimal, time_stamp: LocalDateTime)

object OneFrameRate {
  implicit val ratesFormatter: Format[OneFrameRate] = Json.format[OneFrameRate]
}
