package proxy.forex.entity

import java.time.LocalDateTime

import play.api.libs.json.{Format, Json}
import proxy.forex.model.CurrencyValues.Currency

case class RatesResponse(from: Currency, to: Currency, bid: BigDecimal, ask: BigDecimal, price: BigDecimal, timestamp: LocalDateTime)

object RatesResponse {

  def apply(frameRate: OneFrameRate): RatesResponse = new RatesResponse(frameRate.from, frameRate.to, frameRate.bid, frameRate.ask, frameRate.price, frameRate.time_stamp)

  def apply(frameRate: Seq[OneFrameRate]): Seq[RatesResponse] = frameRate.map(RatesResponse(_))

  implicit val ratesResponseFormatter: Format[RatesResponse] = Json.format[RatesResponse]
}
