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
import play.twirl.api.Html
import uk.gov.hmrc.play.http
import uk.gov.hmrc.play.http._

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

  val statusCodes = Gen.choose(0, 599)
  val successStatusCodes = Gen.choose(200, 299)

  def aPassthroughForSuccessCodes(httpReads: PartialHttpReads[_]) {
    "return None if the status code is between 200 and 299" in {
      forAll(successStatusCodes) { statusCode: Int =>
        val expectedResponse = HttpResponse(statusCode)
        httpReads.read(exampleVerb, exampleUrl, expectedResponse) should be(None)
      }
    }
  }

  def theStandardErrorHandling(reads: HttpReads[_]) {
    theStandardErrorHandlingFor400(reads)
    theStandardErrorHandlingFor404(reads)
    theStandardErrorHandlingForOtherCodes(reads)
  }

  def theStandardErrorHandlingForOtherCodes(reads: http.HttpReads[_]): Unit = {
    "throw the correct exception for all other status codes" in {
      forAll(Gen.choose(0, 199))(expectA[Exception](_)(reads))
      forAll(Gen.choose(400, 499).suchThat(!Seq(400, 404).contains(_)))(expectA[Upstream4xxResponse](_, Some(500))(reads))
      forAll(Gen.choose(500, 599))(expectA[Upstream5xxResponse](_, Some(502))(reads))
      forAll(Gen.choose(600, 1000))(expectA[Exception](_)(reads))
    }
  }

  def theStandardErrorHandlingFor404(reads: http.HttpReads[_]): Unit = {
    "throw the correct exception if the status code is 404" in {
      expectA[NotFoundException](forStatus = 404)(reads)
    }
  }

  def theStandardErrorHandlingFor400(reads: http.HttpReads[_]): Unit = {
    "throw the correct exception if the status code is 400" in {
      expectA[BadRequestException](forStatus = 400)(reads)
    }
  }

  def expectA[T: Manifest](forStatus: Int, reportStatus: Option[Int] = None)(httpReads: HttpReads[_]) {
    val e = the [Exception] thrownBy httpReads.read(exampleVerb, exampleUrl, HttpResponse(forStatus, responseString = Some(exampleBody)))
    e should be (a [T])
    e.getMessage should (include (exampleUrl) and include (exampleVerb) and include (exampleBody))
    reportStatus.foreach { s =>
      e should have ('upstreamResponseCode (forStatus))
      e should have ('reportAs (s))
    }
  }

  def failTheTest[O]: HttpReads[O] = HttpReads[O] { (_, _, _) => fail("Reading passed through to terminator when not expected") }
}
case class Example(v1: String, v2: Int)
