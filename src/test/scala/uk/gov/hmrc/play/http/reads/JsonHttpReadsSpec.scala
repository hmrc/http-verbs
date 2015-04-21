/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http.reads

import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{JsValidationException, HttpResponse}

class JsonHttpReadsSpec extends HttpReadsSpec {
  implicit val r = Json.reads[Example]
  "JsonHttpReads.readFromJson" should {
    val reads = JsonHttpReads.readFromJson[Example]
    "convert a successful response body to the given class" in forAll (`2xx`) { status =>
      val response = HttpResponse(status, responseJson = Some(Json.obj("v1" -> "test", "v2" -> 5)))
      reads.read(exampleVerb, exampleUrl, response) should be(Example("test", 5))
    }
    "convert a successful response body with json that doesn't validate into an exception" in forAll (`2xx`) { status =>
      val response = HttpResponse(status, responseJson = Some(Json.obj("v1" -> "test")))
      a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }
    behave like theStandardErrorHandling (reads)
  }
  "JsonHttpReads.readSeqFromJsonProperty" should {
    val reads = JsonHttpReads.readSeqFromJsonProperty[Example]("items")
    "convert a successful response body to the given class" in forAll (`2xx`.suchThat(_ != 204)) { status =>
      val response = HttpResponse(status, responseJson = Some(
        Json.obj("items" ->
          Json.arr(
            Json.obj("v1" -> "test", "v2" -> 1),
            Json.obj("v1" -> "test", "v2" -> 2)
          )
        )
      ))
      reads.read(exampleVerb, exampleUrl, response) should
        contain theSameElementsInOrderAs Seq(Example("test", 1), Example("test", 2))
    }
    "convert a successful response body with json that doesn't validate into an exception" in forAll (`2xx`.suchThat(_ != 204)) { status =>
      val response = HttpResponse(status, responseJson = Some(
        Json.obj("items" ->
          Json.arr(
            Json.obj("v1" -> "test"),
            Json.obj("v1" -> "test", "v2" -> 2)
          )
        )
      ))
      a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }
    "convert a successful response body with json that is missing the given property into an exception" in forAll (`2xx`.suchThat(_ != 204)) { status =>
      val response = HttpResponse(status, responseJson = Some(
        Json.obj("missing" ->
          Json.arr(
            Json.obj("v1" -> "test", "v2" -> 1),
            Json.obj("v1" -> "test", "v2" -> 2)
          )
        )
      ))
      a[JsValidationException] should be thrownBy reads.read(exampleVerb, exampleUrl, response)
    }
    "return None if the status code is 204 or 404" in {
      reads.read(exampleVerb, exampleUrl, HttpResponse(204)) should be(empty)
      reads.read(exampleVerb, exampleUrl, HttpResponse(404)) should be(empty)
    }
    behave like theStandardErrorHandlingFor400 (reads)
    behave like theStandardErrorHandlingForOtherCodes (reads)
  }
}