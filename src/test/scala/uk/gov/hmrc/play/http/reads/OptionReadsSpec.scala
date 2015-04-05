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

class OptionReadsSpec extends HttpReadsSpec {
  "OptionHttpReads" should {
    val reads = new OptionHttpReads with StubThatShouldNotBeCalled
    "return None if the status code is 204 or 404" in {
      val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = fail("called the nested reads")
      }

      reads.readOptionOf(otherReads).read(exampleVerb, exampleUrl, HttpResponse(204)) should be(None)
      reads.readOptionOf(otherReads).read(exampleVerb, exampleUrl, HttpResponse(404)) should be(None)
    }
    "defer to the nested reads otherwise" in {
      val otherReads = new HttpReads[String] {
        def read(method: String, url: String, response: HttpResponse) = "hi"
      }

      forAll(Gen.posNum[Int].filter(_ != 204).filter(_ != 404)) { s =>
        reads.readOptionOf(otherReads).read(exampleVerb, exampleUrl, HttpResponse(s)) should be(Some("hi"))
      }
    }
    "pass through any failure" in {
      val reads = new OptionHttpReads with StubThatThrowsAnException
      an[Exception] should be thrownBy reads.readOptionOf.read(exampleVerb, exampleUrl, exampleResponse)
    }
  }
}