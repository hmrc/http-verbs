/*
 * Copyright 2015 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json.Writes
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future

class HttpPatchSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  class StubbedHttpPatch(doPatchResult: Future[HttpResponse]) extends HttpPatch with ConnectionTracingCapturing with MockAuditing {
    def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier)= doPatchResult
  }

  "HttpPatch" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPatch = new StubbedHttpPatch(Future.successful(response))
      testPatch.PATCH(url, testObject).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testPatch = new StubbedHttpPatch(Future.successful(new DummyHttpResponse(testBody, 200)))
      testPatch.PATCH(url, testObject).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testPatch = new StubbedHttpPatch(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPatch.PATCH[TestRequestClass, TestClass](url, testObject).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(PATCH, (url, responseF) => new StubbedHttpPatch(responseF).PATCH(url, testObject))
    behave like aTracingHttpCall(PATCH, "PATCH", new StubbedHttpPatch(defaultHttpResponse)) { _.PATCH(url, testObject) }
  }
}
