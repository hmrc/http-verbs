package uk.gov.hmrc.play.http

import org.scalatest.concurrent.ScalaFutures
import play.api.http.HttpVerbs._
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class HttpGetSpec extends UnitSpec with WithFakeApplication with ScalaFutures with CommonHttpBehaviour {

  class TestHttpGet(doGetResult: Future[HttpResponse] = defaultHttpResponse) extends HttpGet with ConnectionTracingCapturing {
    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = doGetResult

    override protected def auditRequestWithResponseF(url: String, verb:String, body:Option[_] ,responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier)= {}
  }
  lazy val TestGET = new TestHttpGet()

  case class TestClass(foo: String, bar: Int)

  implicit val reads = Json.format[TestClass]
  implicit val hc = HeaderCarrier()
  val testBody = "testBody"

  "handleGETResponse" should {
    "decode a valid 200 json response successfully" in {

      val testData = TestClass("foovalue", 123)
      val jsonResponse = Json.toJson(testData).toString()

      val response = new DummyHttpResponse(jsonResponse, 200)


      val url = "http://some.called.url"
      val result = TestGET.handleResponse(GET, url)(response)

      await(result.json) shouldBe Json.toJson(testData)
    }

    "throw an NotFound exception when the response has 404 status" in {
      val response = new DummyHttpResponse(testBody, 404)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[NotFoundException] {
        await(TestGET.handleResponse(GET, url)(response))
      }

      e.getMessage should startWith(GET)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val response = new DummyHttpResponse(testBody, 400)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[BadRequestException] {
        await(TestGET.handleResponse(GET, url)(response))
      }

      e.getMessage should startWith("GET")
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(GET, (url, result) => new TestHttpGet(result).GET[String](url))
    behave like aTracingHttpCall(GET, "GET", new TestHttpGet(response(Some(""""test"""")))) {_.GET[String]("http://some.url")}

    "throw an Exception when the response has an arbitrary status" in {
      val status = 500
      val response = new DummyHttpResponse(testBody, status)

      val url: String = "http://some.nonexistent.url"
      val e = intercept[Exception] {
        await(TestGET.handleResponse(GET, url)(response))
      }

      e.getMessage should startWith(GET)
      e.getMessage should include(url)
      e.getMessage should include("500")
      e.getMessage should include(testBody)
    }

    "get the underlying response with headers, allowing for breakout when the underlying body/headers are required with auditing and tracing" in {
      val status = 200
      val testData = "<p>Partial HTML.</p>"
      val dummyResponse = new DummyHttpResponse(testData, status, Map("X-Header" -> Seq("Value")))

      val url = "http://some.called.url"
      val testGet = new HttpGet {
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(dummyResponse)
        override protected def auditRequestWithResponseF(url: String, verb:String, body:Option[_] ,responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {}
      }
      val result = testGet.GET(url, (responseF, url) => responseF)(null, null, hc)

      await(result).body shouldBe testData
      await(result).header("X-Header") should contain ("Value")
    }
  }

  case class Value(value: String)
  implicit val readValue = Json.reads[Value]

  "GET collection" should {

    val url: String = "http://some.nonexistent.url"

    implicit val hc = HeaderCarrier()

    "Allow a collection of values to be deserialised" in {
      val response = Some(Json.parse("""{ "values" : [{"value" : "something"}]}"""))

      val testGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(200, response))
      }

      val values: Seq[Value] = testGet.GET_Collection[Value](url, "values").futureValue

      values shouldBe Seq(Value("something"))
    }

    "Allow an empty collection to be deserialised" in {
      val response = None

      val testGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(404, response))
      }
      val values = testGet.GET_Collection[Value](url, "values").futureValue

      values shouldBe Seq.empty
    }
    behave like anErrorMappingHttpCall(GET, (url, result) => new TestHttpGet(result).GET_Collection[String](url, "values"))
    behave like aTracingHttpCall(GET, "GET_Collection", new TestHttpGet(response(Some("""{"values" : [] }""")))) {_.GET_Collection[String]("http://some.url", "values")}
  }

  "GET optional" should {
    val url: String = "http://some.nonexistent.url"

    implicit val hc = HeaderCarrier()

    "Allow a value to be deserialised" in {
      val response = Some(Json.parse("""{"value" : "something"}"""))

      val testGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(200, response))
      }

      val values: Option[Value] = testGet.GET_Optional[Value](url).futureValue

      values shouldBe Some(Value("something"))
    }

    "Allow no value to be deserialised" in {
      val response = None

      val testGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(404, response))
      }
      val values = testGet.GET_Optional[Value](url).futureValue

      values shouldBe None
    }
    behave like anErrorMappingHttpCall(GET, (url, result) => new TestHttpGet(result).GET_Optional[String](url))
    behave like aTracingHttpCall(GET, "GET_Optional", new TestHttpGet(response(None, 204))) {_.GET_Optional[String]("http://some.url")}
  }
}
