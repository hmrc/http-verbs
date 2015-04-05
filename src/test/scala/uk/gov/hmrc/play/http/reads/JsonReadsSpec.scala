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

class JsonReadsSpec extends HttpReadsSpec {
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
      a[JsValidationException] should be thrownBy reads.readFromJson[Example].read(exampleVerb, exampleUrl, response)
    }
    "pass through any failure" in {
      val reads = new JsonHttpReads with StubThatThrowsAnException
      an[Exception] should be thrownBy reads.readFromJson[Example].read(exampleVerb, exampleUrl, exampleResponse)
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
      a[JsValidationException] should be thrownBy reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, response)
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
      a[JsValidationException] should be thrownBy reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, response)
    }
    "return None if the status code is 204 or 404" in {
      val reads = new JsonHttpReads with StubThatShouldNotBeCalled
      reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, HttpResponse(204)) should be(empty)
      reads.readSeqFromJsonProperty[Example]("items").read(exampleVerb, exampleUrl, HttpResponse(404)) should be(empty)
    }
    "pass through any failure" in {
      val reads = new JsonHttpReads with StubThatThrowsAnException
      an[Exception] should be thrownBy reads.readFromJson[Example].read(exampleVerb, exampleUrl, exampleResponse)
    }
  }
}