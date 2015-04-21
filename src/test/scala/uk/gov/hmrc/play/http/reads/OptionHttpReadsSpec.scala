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
import uk.gov.hmrc.play.http.HttpResponse

class OptionHttpReadsSpec extends HttpReadsSpec {
  "OptionHttpReads.readOptionOf" should {
    "return None if the status code is 204 or 404" in new TestCase {
      forAll(responsesWith(statusCodes = Gen.oneOf(204, 404)))(noneShouldBeReturnedBy(reads))
      nestedReads should not be 'called
    }
    "defer to the nested reads for other codes" in new TestCase {
      forAll(responsesWith(statusCodes = allValid.suchThat(_ != 204).suchThat(_ != 404)))(someShouldBeReturnedBy(reads))
      nestedReads should be ('called)
    }
  }

  "OptionHttpReads.noneOn" should {
    def reads(status: Int) = OptionHttpReads.noneOn(status)

    "return none on the specified code" in
      forAll(validResponses)(r => noneShouldBeReturnedBy(reads(r.status) or failTheTest[None.type])(r))
    "pass through responses with non-specified codes" in
      forAll(validResponses)(r => theResponseShouldBePassedThroughBy(reads(r.status + 1))(r))
  }

  "OptionHttpReads.some" should {
    "defer to the nested reads for all codes" in new TestCase {
      forAll(validResponses)(someShouldBeReturnedBy(OptionHttpReads.some[String](nestedReads)))
    }
  }

  def someShouldBeReturnedBy(reads: HttpReads[Option[String]])(response: HttpResponse) {
    reads.read(exampleVerb, exampleUrl, response) should be (Some("hi"))
  }

  def noneShouldBeReturnedBy(reads: HttpReads[_ <: Option[String]])(response: HttpResponse) {
    reads.read(exampleVerb, exampleUrl, response) should be (None)
  }


  trait TestCase {
    val nestedReads = new HttpReads[String] {
      var called = false

      def read(method: String, url: String, response: HttpResponse) = {
        called = true
        "hi"
      }
    }
    val reads = OptionHttpReads.readOptionOf(nestedReads)
  }
}
