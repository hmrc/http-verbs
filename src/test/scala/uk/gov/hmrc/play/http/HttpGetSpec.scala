
package uk.gov.hmrc.play.http

import org.scalatest.concurrent.ScalaFutures
import play.api.http.HttpVerbs._
import play.api.libs.json._
import play.twirl.api.Html
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
  val url = "http://some.url"

  "GET" should {
    "decode a valid 200 json response successfully" in {
      val testData = TestClass("foovalue", 123)
      val jsonResponse = Json.toJson(testData).toString()

      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(jsonResponse, 200))
      }

      httpGet.GET[TestClass](url).futureValue shouldBe testData
    }

    "throw an NotFound exception when the response has 404 status" in {
      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(testBody, 404))
      }

      val e = httpGet.GET[TestClass](url).failed.futureValue

      e should be(a[NotFoundException])
      e.getMessage should startWith(GET)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(testBody, 400))
      }

      val e = httpGet.GET[TestClass](url).failed.futureValue

      e should be(a[BadRequestException])
      e.getMessage should startWith("GET")
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(GET, (url, result) => new TestHttpGet(result).GET[String](url))
    behave like aTracingHttpCall(GET, "GET", new TestHttpGet(response(Some(""""test"""")))) {_.GET[String](url)}

    "throw an Exception when the response has an arbitrary status" in {
      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(testBody, 699))
      }

      val e = httpGet.GET[TestClass](url).failed.futureValue

      e should be(a[Exception])
      e.getMessage should startWith(GET)
      e.getMessage should include(url)
      e.getMessage should include("699")
      e.getMessage should include(testBody)
    }

    "get the underlying response with headers, allowing for breakout when the underlying body/headers are required with auditing and tracing" in {
      val status = 200
      val testData = "<p>Partial HTML.</p>"
      val dummyResponse = new DummyHttpResponse(testData, status, Map("X-Header" -> Seq("Value")))

      val url = "http://some.called.url"
      val testGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(dummyResponse)
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
    behave like aTracingHttpCall(GET, "GET_Collection", new TestHttpGet(response(Some("""{"values" : [] }""")))) {_.GET_Collection[String](url, "values")}
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
    behave like aTracingHttpCall(GET, "GET_Optional", new TestHttpGet(response(None, 204))) {_.GET_Optional[String](url)}
  }

  "GET[Option[_]]" ignore {
    val url: String = "http://some.nonexistent.url"

    implicit val hc = HeaderCarrier()

    "Allow a value to be deserialised" in {
      val response = Some(Json.parse("""{"value" : "something"}"""))

      val testGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(200, response))
      }

      val values: Option[Value] = testGet.GET[Option[Value]](url).futureValue

      values shouldBe Some(Value("something"))
    }

    "Allow no value to be deserialised" in {
      val response = None

      val testGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(404, response))
      }
      val values = testGet.GET[Option[Value]](url).futureValue

      values shouldBe None
    }
    behave like anErrorMappingHttpCall(GET, (url, result) => new TestHttpGet(result).GET[Option[String]](url))
    behave like aTracingHttpCall(GET, "GET[Option[_]]", new TestHttpGet(response(None, 204))) {_.GET[Option[String]](url)}
  }

  "GET of non-Json payload" should {
    val exampleHtml = "<h1>Hello Mum</h1>"
    "read HTML" in {

      val httpGet = new HttpGet with ConnectionTracingCapturing {
        protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(new DummyHttpResponse(exampleHtml, 200))
      }

      httpGet.GET[Html](url).futureValue should (
        be (an [Html]) and have ('text (exampleHtml))
      )
    }
    "throw an NotFound exception when the response has 404 status" in {
      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(exampleHtml, 404))
      }

      val e = httpGet.GET[Html](url).failed.futureValue

      e should be(a[NotFoundException])
      e.getMessage should startWith(GET)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(exampleHtml)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(exampleHtml, 400))
      }

      val e = httpGet.GET[Html](url).failed.futureValue

      e should be(a[BadRequestException])
      e.getMessage should startWith("GET")
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(exampleHtml)
    }

    behave like anErrorMappingHttpCall(GET, (url, result) => new TestHttpGet(result).GET[String](url))
    behave like aTracingHttpCall(GET, "GET", new TestHttpGet(response(Some(""""test"""")))) {_.GET[String](url)}

    "throw an Exception when the response has an arbitrary status" in {
      val httpGet = new HttpGet {
        def doGet(url: String)(implicit hc: HeaderCarrier) = Future.successful(new DummyHttpResponse(exampleHtml, 699))
      }

      val e = httpGet.GET[Html](url).failed.futureValue

      e should be(a[Exception])
      e.getMessage should startWith(GET)
      e.getMessage should include(url)
      e.getMessage should include("699")
      e.getMessage should include(exampleHtml)
    }
  }
}
