package proxy.forex

import java.util.concurrent.Executors

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

trait TestUtils extends FlatSpec with org.scalatest.mockito.MockitoSugar with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))


}