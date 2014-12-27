package uk.gov.hmrc.play.http

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.{LoggingDetails, ConnectionTracing}
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.mutable
import scala.concurrent.Future

trait CommonHttpBehaviour extends ScalaFutures {
  this: UnitSpec =>

  def response(returnValue: Option[String] = None, statusCode: Int = 200) = Future.successful(HttpResponse(statusCode, returnValue.map(Json.parse(_))))

  val defaultHttpResponse = response()

  def anErrorMappingHttpCall(verb: String, httpCall: (String, Future[HttpResponse]) => Future[_])= {
    s"throw a GatewayTimeout exception when the HTTP $verb throws a TimeoutException" in {

      implicit val hc = HeaderCarrier()
      val url: String = "http://some.nonexistent.url"

      val e = httpCall(url, Future.failed(new TimeoutException("timeout"))).failed.futureValue

      e should be (a [GatewayTimeoutException])
      e.getMessage should startWith(verb)
      e.getMessage should include(url)
    }

    s"throw a BadGateway exception when the HTTP $verb throws a ConnectException" in {

      implicit val hc = HeaderCarrier()
      val url: String = "http://some.nonexistent.url"

      val e = httpCall(url, Future.failed(new ConnectException("timeout"))).failed.futureValue

      e should be (a [BadGatewayException])
      e.getMessage should startWith(verb)
      e.getMessage should include(url)
    }
  }

  def aTracingHttpCall[T <: ConnectionTracingCapturing](verb: String, method: String, httpBuilder: => T)(httpAction: (T => Future[_]))(implicit mf: Manifest[T]) = {
    s"trace exactly once when the HTTP $verb calls $method" in {
      val http = httpBuilder
      httpAction(http).futureValue
      http.traceCalls should have size 1
      http.traceCalls.head._1 shouldBe verb
    }
  }
}

trait ConnectionTracingCapturing extends ConnectionTracing {

  val traceCalls = mutable.Buffer[(String, String)]()

  override def withTracing[T](method: String, uri: String)(body: => Future[T])(implicit ld: LoggingDetails) = {
    traceCalls += ((method, uri))
    body
  }
}
