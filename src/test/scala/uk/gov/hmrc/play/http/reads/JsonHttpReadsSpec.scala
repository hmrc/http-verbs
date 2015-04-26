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

import org.scalacheck.Gen
import play.api.libs.json._
import uk.gov.hmrc.play.http.{JsValidationException, HttpResponse}

class JsonHttpReadsSpec extends HttpReadsSpec {
  "JsonHttpReads.jsonBodyDeserializedTo" should {
    val reads = JsonHttpReads.jsonBodyDeserialisedTo[Example]
    "convert a response body to the given class" in
      forAll (responsesWith(bodyAsJson = exampleJsonObj))(aExampleClassShouldBeDeserialisedBy(reads))
    "convert a response body with json that doesn't validate into an exception" in
      forAll (responsesWith(bodyAsJson = brokenJsonObj))(expectAJsValidationExceptionFrom(reads))
  }

  "JsonHttpReads.atPath" should {
    val reads = JsonHttpReads.atPath("v1")(HttpReads { (_,_,r) => r })
    "restrict the json response to the given path" in
      forAll (responsesWith(bodyAsJson = Json.obj("v1" -> "test"))) { response =>
        reads.read(exampleVerb, exampleUrl, response) should have (
          'status (response.status),
          'json (JsString("test")),
          'allHeaders (response.allHeaders)
        )
      }
    "generate an exception if the given path is missing" in
      forAll (responsesWith(bodyAsJson = Json.obj("someOtherProp" -> "test"))){ response =>
        reads.read(exampleVerb, exampleUrl, response).json should be (a [JsUndefined])
      }
  }

  "JsonHttpReads.readSeqFromJsonProperty" should {
    val reads = JsonHttpReads.readSeqFromJsonProperty[Example]("items")
    "convert a successful response body to the given class" in
      forAll (responsesWith(`2xx`.suchThat(_ != 204), bodyAsJson = Json.obj("items" -> Json.arr(exampleJsonObj, anotherJsonObj)))) {
        aSeqOfExampleClassesShouldBeDeserializedBy(reads)
      }
    "convert a successful response body with json that doesn't validate into an exception" in
      forAll (responsesWith(`2xx`.suchThat(_ != 204), bodyAsJson = Json.obj("items" -> Json.arr(brokenJsonObj)))) {
         expectAJsValidationExceptionFrom(reads)
      }
    "convert a successful response body with json that is missing the given property into an exception" in forAll (`2xx`.suchThat(_ != 204)) { status =>
      forAll (responsesWith(`2xx`.suchThat(_ != 204), bodyAsJson = Json.obj("missing" -> exampleJsonObj))) {
        expectAJsValidationExceptionFrom(reads)
      }
    }
    "return None if the status code is 204 or 404" in
      forAll (responsesWith(Gen.oneOf(204, 404))){emptySeqShouldBeReturnedBy(reads)}

    behave like theStandardErrorHandlingFor400 (reads)
    behave like theStandardErrorHandlingForOtherCodes (reads)
  }
}
