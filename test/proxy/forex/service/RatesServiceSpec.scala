package proxy.forex.service

import play.api.cache.AsyncCacheApi
import proxy.forex.TestUtils
import proxy.forex.client.OneFrameClient
import proxy.forex.services.RatesServiceImpl
import java.time.LocalDateTime
import java.util.concurrent.Executors

import akka.Done
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when, times}
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import proxy.forex.TestUtils
import proxy.forex.client.OneFrameClient.{OneFrameClientError, UnsupportedOperation}
import proxy.forex.entity.{OneFrameRate, RatesResponse}
import proxy.forex.model.CurrencyPair
import proxy.forex.model.CurrencyValues._
import proxy.forex.services.RatesService.{CacheIsNotReady, CacheUpdateError}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class RatesServiceSpec extends TestUtils {

  private val client = mock[OneFrameClient]
  private val asyncCacheApi = mock[AsyncCacheApi]

  private val sut = new RatesServiceImpl(client, asyncCacheApi)

  private val date = "2020-07-15T18:35:54.719"

  private val cacheKey = "rates"

  "convert" should "take data frm the cache if cache is not empty" in {
    val cache = Seq(OneFrameRate(USD, EUR, BigDecimal(1), BigDecimal(1), BigDecimal(10.00), LocalDateTime.parse(date)),
      OneFrameRate(EUR, USD, BigDecimal(1), BigDecimal(1), BigDecimal(10.00), LocalDateTime.now()))
    when(asyncCacheApi.get[Seq[OneFrameRate]](cacheKey)).thenReturn(Future.successful(Some(cache)))

    Await.result(sut.convert(Seq(CurrencyPair(USD, EUR))), 5.seconds) shouldBe
      Seq(RatesResponse(USD, EUR, BigDecimal(1), BigDecimal(1), BigDecimal(10.00), LocalDateTime.parse(date)))
  }

  it should "return error if the cache is empty" in {
    when(asyncCacheApi.get[Seq[OneFrameRate]](cacheKey)).thenReturn(Future.successful(Some(Seq.empty)))
    a[CacheIsNotReady] should be thrownBy Await.result(sut.convert(Seq(CurrencyPair(USD, EUR))), 5.seconds)
  }


  it should "return error is empty response if the cache is NONE" in {
    when(asyncCacheApi.get[Seq[OneFrameRate]](cacheKey)).thenReturn(Future.successful(None))
    a[CacheIsNotReady] should be thrownBy Await.result(sut.convert(Seq(CurrencyPair(USD, EUR))), 5.seconds)
  }

  it should "return runtime exception if the cache fails" in {
    when(asyncCacheApi.get[Seq[OneFrameRate]](cacheKey)).thenReturn(Future.failed(new RuntimeException))
    a[RuntimeException] should be thrownBy Await.result(sut.convert(Seq(CurrencyPair(USD, EUR))), 5.seconds)
  }

  "update" should "update cache" in {
    val permutations = Seq(
      CurrencyPair(USD, GBP),
      CurrencyPair(GBP, USD),
      CurrencyPair(USD, AUD),
      CurrencyPair(AUD, USD),
      CurrencyPair(GBP, AUD),
      CurrencyPair(AUD, GBP))

    val oneFrameResponse = Seq(OneFrameRate(USD, GBP, BigDecimal(1.0), BigDecimal(2.0), BigDecimal(3.0), LocalDateTime.parse(date)),
      OneFrameRate(GBP, USD, BigDecimal(1.0), BigDecimal(2.0), BigDecimal(3.0), LocalDateTime.parse(date)),
      OneFrameRate(USD, AUD, BigDecimal(1.0), BigDecimal(2.0), BigDecimal(3.0), LocalDateTime.parse(date)),
      OneFrameRate(AUD, USD, BigDecimal(1.0), BigDecimal(2.0), BigDecimal(3.0), LocalDateTime.parse(date)),
      OneFrameRate(GBP, AUD, BigDecimal(1.0), BigDecimal(2.0), BigDecimal(3.0), LocalDateTime.parse(date)),
      OneFrameRate(AUD, GBP, BigDecimal(1.0), BigDecimal(2.0), BigDecimal(3.0), LocalDateTime.parse(date))
    )

    when(asyncCacheApi.remove(cacheKey)).thenReturn(Future.successful(Done))
    when(client.fetch(permutations)).thenReturn(Future.successful(oneFrameResponse))
    when(asyncCacheApi.set(cacheKey, oneFrameResponse)).thenReturn(Future.successful(Done))

    Await.result(sut.update(Seq(USD, GBP, AUD)), 5.seconds)

    verify(asyncCacheApi).remove(cacheKey)
  }

  it should "return CacheUpdateError if fetch returns error" in {
    Mockito.reset(asyncCacheApi)
    when(asyncCacheApi.remove(cacheKey)).thenReturn(Future.successful(Done))
    when(client.fetch(any())).thenReturn(Future.failed(new RuntimeException))

    a[CacheUpdateError] should be thrownBy Await.result(sut.update(Seq(USD, GBP, AUD)), 5.seconds)

    verify(asyncCacheApi).remove(cacheKey)
    verify(asyncCacheApi, times(0)).set(any(), any(), any())
  }
}
