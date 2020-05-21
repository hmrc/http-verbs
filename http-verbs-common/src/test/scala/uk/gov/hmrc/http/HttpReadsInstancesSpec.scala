/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.http

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{__, Json, JsError, JsResult, JsSuccess}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpReadsInstancesSpec extends AnyWordSpec with ScalaCheckDrivenPropertyChecks with Matchers {
  "HttpReads[HttpResponse]" should {
    "return the bare response if returned" in {
      val reads = HttpReads[HttpResponse]
      forAll(Gen.posNum[Int]) { s =>
        val response = exampleResponse(s)
        reads.read(exampleVerb, exampleUrl, response) shouldBe response
      }
    }
  }

  "HttpReads[Option[_]]" should {
    "return None if the status code is 204 or 404" in {
      implicit val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = fail("called the nested reads")
      }
      val reads = HttpReads[Option[String]]

      reads.read(exampleVerb, exampleUrl, exampleResponse(204)) shouldBe None
      reads.read(exampleVerb, exampleUrl, exampleResponse(404)) shouldBe None
    }

    "defer to the nested reads otherwise" in {
      implicit val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = "hi"
      }
      val reads = HttpReads[Option[String]]

      forAll(Gen.posNum[Int].filter(_ != 204).filter(_ != 404)) { s =>
        reads.read(exampleVerb, exampleUrl, exampleResponse(s)) shouldBe Some("hi")
      }
    }
  }


  "HttpReads[Either[UpstreamErrorResponse, _]]" should {
    "return Left if is an error code" in {
      implicit val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = fail("called the nested reads")
      }
      val reads = HttpReads[Either[UpstreamErrorResponse, String]]

      forAll(Gen.choose(400, 499)) { s =>
        val errorResponse = exampleResponse(s)
        reads.read(exampleVerb, exampleUrl, errorResponse) shouldBe Left(Upstream4xxResponse(
          message              = s"$exampleVerb of '$exampleUrl' returned ${errorResponse.status}. Response body: '${errorResponse.body}'",
          upstreamResponseCode = errorResponse.status,
          reportAs             = 500,
          headers              = errorResponse.allHeaders
        ))
      }

      forAll(Gen.choose(500, 599)) { s =>
        val errorResponse = exampleResponse(s)
        reads.read(exampleVerb, exampleUrl, errorResponse) shouldBe Left(Upstream5xxResponse(
          message              = s"$exampleVerb of '$exampleUrl' returned ${errorResponse.status}. Response body: '${errorResponse.body}'",
          upstreamResponseCode = errorResponse.status,
          reportAs             = 502,
          headers              = errorResponse.allHeaders
        ))
      }
    }

    "defer to the nested reads otherwise" in {
      implicit val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = "hi"
      }
      val reads = HttpReads[Either[UpstreamErrorResponse, String]]

      forAll(Gen.choose(200, 299)) { s =>
        reads.read(exampleVerb, exampleUrl, exampleResponse(s)) shouldBe Right("hi")
      }
    }
  }

  implicit val r = Json.reads[Example]

  "HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]" should {
    val reads = HttpReads[Either[UpstreamErrorResponse, JsResult[Example]]]

    "convert a successful response body to a JsSuccess" in {
      val response = HttpResponse(200, responseJson = Some(Json.obj("v1" -> "test", "v2" -> 5)))
      reads.read(exampleVerb, exampleUrl, response) shouldBe Right(JsSuccess(Example("test", 5)))
    }

    "convert a successful response body with json that doesn't validate to a JsError" in {
      val response = HttpResponse(200, responseJson = Some(Json.obj("v1" -> "test")))
      reads.read(exampleVerb, exampleUrl, response) shouldBe Right(JsError(__ \ "v2", "error.path.missing"))
    }

    "convert a failed response to an UpstreamErrorResponse" in {
      forAll(Gen.choose(400, 499)) { s =>
        val errorResponse = exampleResponse(s)
        reads.read(exampleVerb, exampleUrl, errorResponse) shouldBe Left(Upstream4xxResponse(
          message              = s"$exampleVerb of '$exampleUrl' returned ${errorResponse.status}. Response body: '${errorResponse.body}'",
          upstreamResponseCode = errorResponse.status,
          reportAs             = 500,
          headers              = errorResponse.allHeaders
        ))
      }

      forAll(Gen.choose(500, 599)) { s =>
        val errorResponse = exampleResponse(s)
        reads.read(exampleVerb, exampleUrl, errorResponse) shouldBe Left(Upstream5xxResponse(
          message              = s"$exampleVerb of '$exampleUrl' returned ${errorResponse.status}. Response body: '${errorResponse.body}'",
          upstreamResponseCode = errorResponse.status,
          reportAs             = 502,
          headers              = errorResponse.allHeaders
        ))
      }
    }
  }

  "JsonHttpReads.readFromJson" should {
    val reads = HttpReads[Example]
    "convert a successful response body to the given class" in {
      val response = HttpResponse(200, responseJson = Some(Json.obj("v1" -> "test", "v2" -> 5)))
      reads.read(exampleVerb, exampleUrl, response) shouldBe Example("test", 5)
    }

    "convert a successful response body with json that doesn't validate into an exception" in {
      val response = HttpResponse(200, responseJson = Some(Json.obj("v1" -> "test")))
      a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }

    "throw Exception for any failure" in {
      forAll(Gen.choose(400, 599)) { s =>
        val errorResponse = exampleResponse(s)
        a[UpstreamErrorResponse] should be thrownBy reads.read(exampleVerb, exampleUrl, errorResponse)
      }
    }
  }

  val exampleVerb = "GET"
  val exampleUrl  = "http://example.com/something"

  def exampleResponse(statusCode: Int) = HttpResponse(
    responseStatus  = statusCode,
    responseJson    = Some(Json.parse("""{"test":1}""")),
    responseHeaders = Map("X-something" -> Seq("some value")),
    responseString  = Some("this is the string body")
  )

  case class Example(v1: String, v2: Int)
}
