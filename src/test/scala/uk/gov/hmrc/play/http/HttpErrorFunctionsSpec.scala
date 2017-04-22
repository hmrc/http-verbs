/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.http

import org.scalacheck.Gen
import org.scalatest.matchers.{MatchResult, Matcher, HavePropertyMatchResult, HavePropertyMatcher}
import org.scalatest.prop.{TableDrivenPropertyChecks, GeneratorDrivenPropertyChecks}
import org.scalatest.{TryValues, Matchers, WordSpec}

import scala.util.{Failure, Try}

class HttpErrorFunctionsSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with TableDrivenPropertyChecks with TryValues {

  "HttpErrorFunctions" should {
    "return the response if the status code is between 200 and 299" in new HttpErrorFunctions {
      forAll (Gen.choose(200, 299)) { statusCode: Int =>
        val expectedResponse = HttpResponse(statusCode)
        handleResponse(exampleVerb, exampleUrl)(expectedResponse) should be (expectedResponse)
      }
    }
    "return the correct exception if the status code is 400" in { expectA[BadRequestException](forStatus = 400) }
    "return the correct exception if the status code is 404" in { expectA[NotFoundException]  (forStatus = 404) }
    "return the correct exception for all other status codes" in {
      forAll (Gen.choose(0, 199))                                        (expectA[Exception](_))
      forAll (Gen.choose(400, 499).suchThat(!Seq(400, 404).contains(_))) (expectA[Upstream4xxResponse](_, Some(500)))
      forAll (Gen.choose(500, 599))                                      (expectA[Upstream5xxResponse](_, Some(502)))
      forAll (Gen.choose(600, 1000))                                     (expectA[Exception](_))
    }
  }

  val exampleVerb = "GET"
  val exampleUrl = "http://example.com/something"
  val exampleBody = "this is the string body"

  def expectA[T: Manifest](forStatus: Int, reportStatus: Option[Int] = None): Unit = new HttpErrorFunctions {
    val e = Try(handleResponse(exampleVerb, exampleUrl)(HttpResponse(forStatus, responseString = Some(exampleBody)))).failure.exception
    e should be (a [T])
    e.getMessage should (include (exampleUrl) and include (exampleVerb) and include (exampleBody))
    reportStatus.map { s =>
      e should have ('upstreamResponseCode (forStatus))
      e should have ('reportAs (s))
    }
  }
}
