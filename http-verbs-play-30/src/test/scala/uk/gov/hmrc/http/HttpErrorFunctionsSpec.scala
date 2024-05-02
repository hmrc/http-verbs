/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalacheck.{Gen, Shrink}
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class HttpErrorFunctionsSpec
  extends AnyWordSpec
     with Matchers
     with ScalaCheckDrivenPropertyChecks
     with TableDrivenPropertyChecks
     with EitherValues {

  // Disable shrinking
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  "HttpErrorFunctions.handleResponseEither" should {
    "return the response if the status code is between 200 and 299" in {
      forAll(Gen.choose(200, 299))(expectResponse)
    }

    "return the exception for 4xx, 5xx" in {
      forAll(Gen.choose(400, 499))(expectError(_, 500))
      forAll(Gen.choose(500, 599))(expectError(_, 502))
    }

    "return the response for other codes" in {
      forAll(Gen.choose(0, 399))(expectResponse)
      forAll(Gen.choose(600, 1000))(expectResponse)
    }
  }

  val exampleVerb = "GET"
  val exampleUrl  = "http://example.com/something"
  val exampleBody = "this is the string body"

  def expectError(statusCode: Int, reportAs: Int): Unit =
    new HttpErrorFunctions {
      val e = handleResponseEither(exampleVerb, exampleUrl)(HttpResponse(statusCode, exampleBody)).left.value
      e.getMessage should (include(exampleUrl) and include(exampleVerb) and include(exampleBody))
      e.statusCode shouldBe statusCode
      e.reportAs   shouldBe reportAs
    }

  def expectResponse(forStatus: Int): Unit =
    new HttpErrorFunctions {
      val expectedResponse = HttpResponse(forStatus, "")
      handleResponseEither(exampleVerb, exampleUrl)(expectedResponse).value shouldBe expectedResponse
    }
}
