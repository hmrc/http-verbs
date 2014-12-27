package uk.gov.hmrc.play.http

import play.api.http.HttpVerbs._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class HttpDeleteSpec extends UnitSpec with WithFakeApplication with CommonHttpBehaviour {

  implicit val hc = HeaderCarrier()

  class TestDelete(doDeleteResult: Future[HttpResponse] = defaultHttpResponse) extends HttpDelete with ConnectionTracingCapturing {

    override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = doDeleteResult

    override protected def auditRequestWithResponseF(url: String, verb:String, body:Option[_] ,responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {}
  }

  lazy val testDelete = new TestDelete()

  "handleDELETEResponse" should {
    val testBody = "testBody"

    "return the endpoint's response when the returned status code is in the 2xx range" in {
      (200 to 299).foreach { status =>
        val response = new DummyHttpResponse(testBody, status)

        val result = testDelete.handleResponse(DELETE, "http://some.url")(response)
        await(result) shouldBe response
      }
    }

    "throw an NotFoundException when the response has 404 status" in {
      val response = new DummyHttpResponse(testBody, 404)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[NotFoundException] {
        await(testDelete.handleResponse(DELETE, url)(response))
      }

      e.getMessage should startWith(DELETE)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val response = new DummyHttpResponse(testBody, 400)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[BadRequestException] {
        await(testDelete.handleResponse(DELETE, url)(response))
      }

      e.getMessage should startWith(DELETE)
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(DELETE, (url, responseF) => new TestDelete(responseF).DELETE(url))
    behave like aTracingHttpCall(DELETE, "DELETE", new TestDelete) { _.DELETE("http://some.url") }

    "throw a Exception when the response has an arbitrary status" in {
      val status = 500
      val response = new DummyHttpResponse(testBody, status)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[Exception] {
        await(testDelete.handleResponse(DELETE, url)(response))
      }

      e.getMessage should startWith(DELETE)
      e.getMessage should include(url)
      e.getMessage should include("500")
      e.getMessage should include(testBody)
    }
  }
}
