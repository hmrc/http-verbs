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
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.play.http
import uk.gov.hmrc.play.http.{HttpResponse, Upstream4xxResponse, Upstream5xxResponse, BadRequestException, NotFoundException}
import http.reads.HttpReads

trait HttpReadsSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  val exampleVerb = "GET"
  val exampleUrl = "http://example.com/something"
  val exampleBody = "this is the string body"
  val exampleResponse = HttpResponse(
    responseStatus = 0,
    responseJson = Some(Json.parse("""{"test":1}""")),
    responseHeaders = Map("X-something" -> Seq("some value")),
    responseString = Some(exampleBody)
  )

  val invalidCodes = Gen.oneOf(Gen.choose(Int.MinValue, 99), Gen.choose(600, Int.MaxValue))
  val allValid = Gen.choose(100, 599)
  val `1xx` = Gen.choose(100, 199)
  val `2xx` = Gen.choose(200, 299)
  val `4xx` = Gen.choose(400, 499)
  val `5xx` = Gen.choose(500, 599)

  val validResponses = responsesWith()
  def responsesWith(statusCodes: Gen[Int] = allValid, bodyAsString: Gen[Option[String]] = Gen.const(Some(exampleBody))) = for {
    status <- statusCodes
    bodyAsString <- bodyAsString
  } yield HttpResponse(status, responseString = bodyAsString)

  def aPassthroughForSuccessCodes(httpReads: PartialHttpReads[_]) {
    "pass through responses where the status code is 2xx" in 
      forAll(responsesWith(`2xx`))(theResponseShouldBePassedThroughBy(httpReads))
  }

  def theStandardErrorHandling(reads: HttpReads[_]) {
    theStandardErrorHandlingFor400(reads)
    theStandardErrorHandlingFor404(reads)
    theStandardErrorHandlingForOtherCodes(reads)
  }

  def theStandardErrorHandlingForOtherCodes(reads: HttpReads[_]) {
    theStandardErrorHandlingForInvalidCodes(reads)
    theStandardErrorHandlingFor1xxCodes(reads)
    theStandardErrorHandlingFor4xxCodes(reads)
    theStandardErrorHandlingFor5xxCodes(reads)
  }

  def theStandardErrorHandlingFor5xxCodes(reads: HttpReads[_]) {
    "throw the correct exception for 5xx status codes" in 
      forAll(responsesWith(`5xx`))(expectA[Upstream5xxResponse](reportedAsStatus = Some(502))(reads))
  }

  def theStandardErrorHandlingFor4xxCodes(reads: HttpReads[_]) {
    "throw the correct exception for 4xx status codes" in
    forAll(responsesWith(`4xx`.suchThat(_ != 400).suchThat(_ != 404)))(expectA[Upstream4xxResponse](reportedAsStatus = Some(500))(reads))
  }

  def theStandardErrorHandlingFor1xxCodes(reads: HttpReads[_]) {
    "throw the correct exception for 1xx status codes" in
      forAll(responsesWith(`1xx`))(expectA[Exception]()(reads))
  }

  def theStandardErrorHandlingForInvalidCodes(reads: HttpReads[_]) {
    "throw the correct exception for invalid status codes" in
      forAll(responsesWith(invalidCodes))(expectA[Exception]()(reads))
  }

  def theStandardErrorHandlingFor404(reads: HttpReads[_]) {
    "throw the correct exception if the status code is 404" in
      forAll(responsesWith(statusCodes = 404))(expectA[NotFoundException]()(reads))
  }

  def theStandardErrorHandlingFor400(reads: HttpReads[_]) {
    "throw the correct exception if the status code is 400" in
      forAll(responsesWith(statusCodes = 400))(expectA[BadRequestException]()(reads))
  }

  def expectA[T: Manifest](reportedAsStatus: Option[Int] = None)(httpReads: HttpReads[_])(response: HttpResponse) {
    val e = the[Exception] thrownBy httpReads.read(exampleVerb, exampleUrl, response)
    e should be(a[T])
    e.getMessage should (include(exampleUrl) and include(exampleVerb) and include(exampleBody))
    reportedAsStatus.foreach { s =>
      e should have('upstreamResponseCode(response.status))
      e should have('reportAs(s))
    }
  }
  
  def theResponseShouldBePassedThroughBy(httpReads: PartialHttpReads[_])(response: HttpResponse) {
    httpReads.read(exampleVerb, exampleUrl, response) should be(None)
  }

  def failTheTest[O]: HttpReads[O] = HttpReads[O] { (_, _, _) => fail("Reading passed through to terminator when not expected") }
}
case class Example(v1: String, v2: Int)
