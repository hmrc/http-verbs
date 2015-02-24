package uk.gov.hmrc.play.http

import play.api.http.HttpVerbs._
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class HttpPostSpec extends UnitSpec with WithFakeApplication with CommonHttpBehaviour {

  implicit val hc = HeaderCarrier()
  class TestPOST(
                  doPostResult: Future[HttpResponse] = defaultHttpResponse,
                  doFormPostResult: Future[HttpResponse] = defaultHttpResponse,
                  doPostStringResult: Future[HttpResponse] = defaultHttpResponse,
                  doPostEmptyResult: Future[HttpResponse] = defaultHttpResponse) extends HttpPost with ConnectionTracingCapturing with MockAuditing {
    override def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = doPostResult

    override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = doFormPostResult

    override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = doPostStringResult

    var auditRequestWithResponseF_callCount = 0
    override protected def auditRequestWithResponseF(url: String, verb:String, body: Option[_], responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {
      auditRequestWithResponseF_callCount = auditRequestWithResponseF_callCount + 1
    }

    protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = doPostEmptyResult
  }

  lazy val testPOST = new TestPOST()

  val someUrl = "http://some.url"
  "handlePOSTResponse" should {

    "return the endpoint's response when the returned status code is in the 2xx range" in {
      (200 to 299).foreach {
        status =>
          val response = new DummyHttpResponse("", status)

          val result = testPOST.handleResponse(POST, someUrl)(response)

          await(result) shouldBe response
      }
    }

    val testBody = "testBody"

    "throw an NotFoundException when the response has 404 status" in {
      val response = new DummyHttpResponse(testBody, 404)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[NotFoundException] {
        await(testPOST.handleResponse(POST, url)(response))
      }

      e.getMessage should startWith(POST)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val response = new DummyHttpResponse(testBody, 400)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[BadRequestException] {
        await(testPOST.handleResponse(POST, url)(response))
      }

      e.getMessage should startWith(POST)
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(POST, (url, responseF) => new TestPOST(doPostResult = responseF).POST(url, "anyString"))
    behave like aTracingHttpCall[TestPOST](POST, "POST", new TestPOST) { _.POST(someUrl, "anyString") }

    "throw a Exception when the response has an arbitrary status" in {
      val status = 500
      val response = new DummyHttpResponse(testBody, status)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[Exception] {
        await(testPOST.handleResponse(POST, url)(response))
      }

      e.getMessage should startWith(POST)
      e.getMessage should include(url)
      e.getMessage should include("500")
      e.getMessage should include(testBody)
    }
  }

  "POSTForm" should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new TestPOST(doFormPostResult = responseF).POSTForm(url, Map()))
    behave like aTracingHttpCall[TestPOST](POST, "POSTForm", new TestPOST) { _.POSTForm(someUrl, Map()) }
  }

  "POSTString"  should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new TestPOST(doPostStringResult = responseF).POSTString(url, "body", Seq.empty))
    behave like aTracingHttpCall[TestPOST](POST, "POSTString", new TestPOST) { _.POSTString(someUrl, "body", Seq.empty) }
  }

  "POSTEmpty"  should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new TestPOST(doPostEmptyResult = responseF).POSTEmpty(url))
    behave like aTracingHttpCall[TestPOST](POST, "POSTEmpty", new TestPOST) { _.POSTEmpty(someUrl) }
  }

}
