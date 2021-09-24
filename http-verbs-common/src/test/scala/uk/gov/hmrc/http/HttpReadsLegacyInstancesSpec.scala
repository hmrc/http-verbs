/*
 * Copyright 2022 HM Revenue & Customs
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

import com.github.ghik.silencer.silent
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import _root_.play.api.libs.json.Json

@silent("deprecated")
class HttpReadsLegacyInstancesSpec extends AnyWordSpec with ScalaCheckDrivenPropertyChecks with Matchers {
  "RawReads" should {
    val reads = HttpReads.readRaw
    "return the bare response if success" in {
      forAll(Gen.choose(200, 299)) { s =>
        val response = exampleResponse(s)
        reads.read(exampleVerb, exampleUrl, response) should be(response)
      }
    }
    "throw exception for non-success codes" in {
      forAll(Gen.choose(400, 599)) { s =>
        val errorResponse = exampleResponse(s)
        an[Exception] should be thrownBy reads.read(exampleVerb, exampleUrl, errorResponse)
      }
    }
  }

  "OptionHttpReads" should {
    "return None if the status code is 204 or 404" in {
      val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = fail("called the nested reads")
      }
      val reads = HttpReads.readOptionOf(otherReads)

      reads.read(exampleVerb, exampleUrl, exampleResponse(204)) should be(None)
      reads.read(exampleVerb, exampleUrl, exampleResponse(404)) should be(None)
    }

    "defer to the nested reads otherwise" in {
      val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = "hi"
      }
      val reads = HttpReads.readOptionOf(otherReads)

      forAll(Gen.posNum[Int].filter(_ != 204).filter(_ != 404)) { s =>
        reads.read(exampleVerb, exampleUrl, exampleResponse(s)) should be(Some("hi"))
      }
    }
  }

  implicit val r = Json.reads[Example]
  "JsonHttpReads.readFromJson" should {
    val reads = HttpReads.readFromJson[Example]
    "convert a successful response body to the given class" in {
      val response = HttpResponse(200, responseJson = Some(Json.obj("v1" -> "test", "v2" -> 5)))
      reads.read(exampleVerb, exampleUrl, response) should be(Example("test", 5))
    }

    "convert a successful response body with json that doesn't validate into an exception" in {
      val response = HttpResponse(200, responseJson = Some(Json.obj("v1" -> "test")))
      a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }

    "throw Exception for any failure" in {
      forAll(Gen.posNum[Int].filter(!_.toString.startsWith("2"))) { s =>
        an[Exception] should be thrownBy reads.read(exampleVerb, exampleUrl, exampleResponse(s))
      }
    }
  }

  "JsonHttpReads.readSeqFromJsonProperty" should {
    val reads = HttpReads.readSeqFromJsonProperty[Example]("items")
    "convert a successful response body to the given class" in {
      val response = HttpResponse(
        200,
        responseJson = Some(
          Json.obj(
            "items" ->
              Json.arr(
                Json.obj("v1" -> "test", "v2" -> 1),
                Json.obj("v1" -> "test", "v2" -> 2)
              ))
        ))
      reads.read(exampleVerb, exampleUrl, response) should
        contain theSameElementsInOrderAs Seq(Example("test", 1), Example("test", 2))
    }

    "convert a successful response body with json that doesn't validate into an exception" in {
      val response = HttpResponse(
        200,
        responseJson = Some(
          Json.obj(
            "items" ->
              Json.arr(
                Json.obj("v1" -> "test"),
                Json.obj("v1" -> "test", "v2" -> 2)
              ))
        ))
       a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }

    "convert a successful response body with json that is missing the given property into an exception" in {
      val response = HttpResponse(
        200,
        responseJson = Some(
          Json.obj(
            "missing" ->
              Json.arr(
                Json.obj("v1" -> "test", "v2" -> 1),
                Json.obj("v1" -> "test", "v2" -> 2)
              ))
        ))
      a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }

    "return None if the status code is 204 or 404" in {
      reads.read(exampleVerb, exampleUrl, exampleResponse(204)) should be(empty)
      reads.read(exampleVerb, exampleUrl, exampleResponse(404)) should be(empty)
    }

    "throw an Exception for any failure" in {
      forAll(Gen.posNum[Int].filter(!_.toString.startsWith("2"))) { s =>
        an[Exception] should be thrownBy reads.read(exampleVerb, exampleUrl, exampleResponse(s))
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
