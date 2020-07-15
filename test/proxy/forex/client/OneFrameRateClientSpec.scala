package proxy.forex.client

import java.time.LocalDateTime

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import proxy.forex.TestUtils
import proxy.forex.client.OneFrameClient.{OneFrameClientError, UnsupportedOperation}
import proxy.forex.entity.OneFrameRate
import proxy.forex.model.CurrencyPair
import proxy.forex.model.CurrencyValues._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class OneFrameRateClientSpec extends TestUtils {

  private val responseBody = "[{\"from\":\"USD\",\"to\":\"JPY\",\"bid\":0.61,\"ask\":0.82,\"price\":0.71,\"time_stamp\":\"2019-01-01T00:00:00.000\"}, {\"from\":\"EUR\",\"to\":\"GBP\",\"bid\":0.33,\"ask\":0.33,\"price\":0.37,\"time_stamp\":\"2019-01-01T00:00:00.000\"}]"

  private val httpClient: WSClient = mock[WSClient]
  private val httpRequest: WSRequest = mock[WSRequest]
  private val response = mock[WSResponse]

  private val Url = "localhost:8080"
  private val Token = "123456"
  private val configuration = Configuration("paidyinc.url" -> Url, "paidyinc.token" -> Token)

  private val sut = new OneFrameClientImpl(httpClient, configuration)
  private val currencyPairs = Seq(CurrencyPair(USD, JPY), CurrencyPair(EUR, GBP))

  "fetch" should "get the list of the rates from the external service" in {
    when(response.status).thenReturn(Status.OK)
    when(response.json).thenReturn(Json.parse(responseBody))
    when(httpRequest.withHttpHeaders(any())).thenReturn(httpRequest)
    when(httpClient.url(anyString())).thenReturn(httpRequest)
    when(httpRequest.get()).thenReturn(Future.successful(response))

    Await.result(sut.fetch(currencyPairs), 5.seconds) shouldBe Seq(
      OneFrameRate(USD, JPY, BigDecimal(0.61), BigDecimal(0.82), BigDecimal(0.71), LocalDateTime.parse("2019-01-01T00:00:00.000")),
      OneFrameRate(EUR, GBP, BigDecimal(0.33), BigDecimal(0.33), BigDecimal(0.37), LocalDateTime.parse("2019-01-01T00:00"))
    )
  }

  it should "fails if the status not OK" in {
    when(response.status).thenReturn(Status.ACCEPTED)
    when(response.json).thenReturn(Json.parse(responseBody))
    when(httpRequest.withHttpHeaders(any())).thenReturn(httpRequest)
    when(httpClient.url(anyString())).thenReturn(httpRequest)
    when(httpRequest.get()).thenReturn(Future.successful(response))

    a[UnsupportedOperation] should be thrownBy {
      Await.result(sut.fetch(currencyPairs), 5.seconds)
    }
  }

  it should "fails if the api response was failed" in {
    when(response.status).thenReturn(Status.OK)
    when(response.json).thenReturn(Json.parse(responseBody))
    when(httpRequest.withHttpHeaders(any())).thenReturn(httpRequest)
    when(httpClient.url(anyString())).thenReturn(httpRequest)
    when(httpRequest.get()).thenReturn(Future.failed(new RuntimeException))

    a[OneFrameClientError] should be thrownBy {
      Await.result(sut.fetch(currencyPairs), 5.seconds)
    }
  }

  it should "fails if the body is invalid" in {
    val responseBody = "{\"from\":\"USD\",\"to\":\"JPY\"}"
    when(response.status).thenReturn(Status.OK)
    when(response.json).thenReturn(Json.parse(responseBody))
    when(httpRequest.withHttpHeaders(any())).thenReturn(httpRequest)
    when(httpClient.url(anyString())).thenReturn(httpRequest)
    when(httpRequest.get()).thenReturn(Future.successful(response))

    a[RuntimeException] should be thrownBy {
      Await.result(sut.fetch(currencyPairs), 5.seconds)
    }
  }

}
