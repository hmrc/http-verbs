
package uk.gov.hmrc.play.http

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json
import play.twirl.api.Html

class HttpReadsSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  "OptionHttpReads" should {
    val reads = new OptionHttpReads with StubThatShouldNotBeCalled
    "return None if the status code is 204 or 404" in {
      val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = fail("called the nested reads")
      }

      reads.readOptionOf(otherReads).read(exampleVerb, exampleUrl, HttpResponse(204)) should be (None)
      reads.readOptionOf(otherReads).read(exampleVerb, exampleUrl, HttpResponse(404)) should be (None)
    }
    "defer to the nested reads otherwise" in {
      val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = "hi"
      }

      forAll(Gen.posNum[Int].filter(_ != 204).filter(_ != 404)) { s =>
        reads.readOptionOf(otherReads).read(exampleVerb, exampleUrl, HttpResponse(s)) should be(Some("hi"))
      }
    }
    "pass through any failure" in {
      val reads = new OptionHttpReads with StubThatThrowsAnException
      an [Exception] should be thrownBy reads.readOptionOf.read(exampleVerb, exampleUrl, exampleResponse)
    }
  }
  "HtmlHttpReads" should {
    "convert a successful response body to HTML" in {
      val reads = new HtmlHttpReads with StubThatReturnsTheResponse

      reads.readToHtml.read(exampleVerb, exampleUrl, HttpResponse(0, responseString = Some("<p>hello</p>"))) should (
        be (an[Html]) and have ('text ("<p>hello</p>"))
      )
    }
    "pass through any failure" in {
      val reads = new HtmlHttpReads with StubThatThrowsAnException
      an [Exception] should be thrownBy reads.readToHtml.read(exampleVerb, exampleUrl, exampleResponse)
    }
  }

  implicit val r = Json.reads[Example]
  "JsonHttpReads.readFromJson" should {
    "convert a successful response body to the given class" in {
      val reads = new JsonHttpReads with StubThatReturnsTheResponse
      val response = HttpResponse(0, responseJson = Some(Json.obj("v1" -> "test", "v2" -> 5)))
      reads.readFromJson[Example].read(exampleVerb, exampleUrl, response) should be(Example("test", 5))
    }
    "convert a successful response body with json that doesn't validate into an exception" in {
      val reads = new JsonHttpReads with StubThatReturnsTheResponse
      val response = HttpResponse(0, responseJson = Some(Json.obj("v1" -> "test")))
      a [JsValidationException] should be thrownBy reads.readFromJson[Example].read(exampleVerb, exampleUrl, response)
    }
    "pass through any failure" in {
      val reads = new JsonHttpReads with StubThatThrowsAnException
      an [Exception] should be thrownBy reads.readFromJson[Example].read(exampleVerb, exampleUrl, exampleResponse)
    }
  }
  "JsonHttpReads.readSeqFromJsonProperty" should {
    "convert a successful response body to the given class" in {
      val reads = new JsonHttpReads with StubThatReturnsTheResponse
      val response = HttpResponse(0, responseJson = Some(
        Json.obj("items" ->
          Json.arr(
            Json.obj("v1" -> "test", "v2" -> 1),
            Json.obj("v1" -> "test", "v2" -> 2)
          )
        )
      ))
      reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, response) should
        contain theSameElementsInOrderAs Seq(Example("test", 1), Example("test", 2))
    }
    "convert a successful response body with json that doesn't validate into an exception" in {
      val reads = new JsonHttpReads with StubThatReturnsTheResponse
      val response = HttpResponse(0, responseJson = Some(
        Json.obj("items" ->
          Json.arr(
            Json.obj("v1" -> "test"),
            Json.obj("v1" -> "test", "v2" -> 2)
          )
        )
      ))
      a [JsValidationException] should be thrownBy reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, response)
    }
    "convert a successful response body with json that is missing the given property into an exception" in {
      val reads = new JsonHttpReads with StubThatReturnsTheResponse
      val response = HttpResponse(0, responseJson = Some(
        Json.obj("missing" ->
          Json.arr(
            Json.obj("v1" -> "test", "v2" -> 1),
            Json.obj("v1" -> "test", "v2" -> 2)
          )
        )
      ))
      a [JsValidationException] should be thrownBy reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, response)
    }
    "return None if the status code is 204 or 404" in {
      val reads = new JsonHttpReads with StubThatShouldNotBeCalled
      reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, HttpResponse(204)) should be (empty)
      reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, HttpResponse(404)) should be (empty)
    }
    "pass through any failure" in {
      val reads = new JsonHttpReads with StubThatThrowsAnException
      an [Exception] should be thrownBy reads.readFromJson[Example].read(exampleVerb, exampleUrl, exampleResponse)
    }
  }

  val exampleVerb = "GET"
  val exampleUrl = "http://example.com/something"
  val exampleBody = "this is the string body"
  val exampleResponse = HttpResponse(0)

  trait StubThatShouldNotBeCalled extends HttpErrorFunctions {
    override def handleResponse(httpMethod: String, url: String)(response: HttpResponse) = {
      fail("called handleResponse when not expected to")
    }
  }

  trait StubThatReturnsTheResponse extends HttpErrorFunctions {
    override def handleResponse(httpMethod: String, url: String)(response: HttpResponse) = response
  }

  trait StubThatThrowsAnException extends HttpErrorFunctions {
    override def handleResponse(httpMethod: String, url: String)(response: HttpResponse) = throw new Exception
  }
}
case class Example(v1: String, v2: Int)
