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
import uk.gov.hmrc.play.test.Concurrent.await
import uk.gov.hmrc.play.test.Concurrent.liftFuture
import scala.concurrent.Future

class HttpPutSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  class StubbedHttpPut(doPutResult: Future[HttpResponse]) extends HttpPut with ConnectionTracingCapturing with MockAuditing {
    def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier)= doPutResult
  }

  "HttpPut" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPut = new StubbedHttpPut(Future.successful(response))
      testPut.PUT(url, testObject).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testPut = new StubbedHttpPut(Future.successful(new DummyHttpResponse(testBody, 200)))
      testPut.PUT(url, testObject).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testPut = new StubbedHttpPut(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPut.PUT[TestRequestClass, TestClass](url, testObject).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(PUT, (url, responseF) => new StubbedHttpPut(responseF).PUT(url, testObject))
    behave like aTracingHttpCall(PUT, "PUT", new StubbedHttpPut(defaultHttpResponse)) { _.PUT(url, testObject) }
  }
}
