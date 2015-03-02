package uk.gov.hmrc.play.http

import play.api.http.HttpVerbs._
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class HttpPutSpec extends UnitSpec with WithFakeApplication with CommonHttpBehaviour {

  implicit val hc = HeaderCarrier()

  class TestPUT(doPutResult: Future[HttpResponse] = defaultHttpResponse) extends HttpPut with ConnectionTracingCapturing with MockAuditing {
    override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = doPutResult

    override protected def auditRequestWithResponseF(url: String, verb:String, body:Option[_] ,responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {}
  }
  
  lazy val testPUT = new TestPUT()

  "handlePUTResponse" should {

    val testBody = "testBody"

    "return the endpoint's response when the returned status code is in the 2xx range" in {
      (200 to 299).foreach { status =>
        val response = new DummyHttpResponse(testBody, status)
        val result = testPUT.handleResponse(PUT, "http://some.url")(response)

        await(result) shouldBe response
      }
    }

    "throw an NotFoundException when the response has 404 status" in {
      val response = new DummyHttpResponse(testBody, 404)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[NotFoundException] {
        await(testPUT.handleResponse(PUT, url)(response))
      }

      e.getMessage should startWith(PUT)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val response = new DummyHttpResponse(testBody, 400)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[BadRequestException] {
        await(testPUT.handleResponse(PUT, url)(response))
      }

      e.getMessage should startWith(PUT)
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(PUT, (url, responseF) => new TestPUT(responseF).PUT[String](url, "testString"))
    behave like aTracingHttpCall(PUT, "PUT", new TestPUT) { _.PUT[String]("http://some.url", "body")}

    "throw a Exception when the response has an arbitrary status" in {
      val status = 500
      val response = new DummyHttpResponse(testBody, status)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[Exception] {
        await(testPUT.handleResponse(PUT, url)(response))
      }

      e.getMessage should startWith(PUT)
      e.getMessage should include(url)
      e.getMessage should include("500")
      e.getMessage should include(testBody)
    }
  }
}
