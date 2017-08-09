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

package uk.gov.hmrc.http

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpPutSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  class StubbedHttpPut(doPutResult: Future[HttpResponse]) extends HttpPut with MockitoSugar with ConnectionTracingCapturing {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier)= doPutResult
  }

  "HttpPut" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPut = new StubbedHttpPut(Future.successful(response))
      testPut.PUT(url, testObject).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPut = new StubbedHttpPut(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPut.PUT[TestRequestClass, TestClass](url, testObject).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall("PUT", (url, responseF) => new StubbedHttpPut(responseF).PUT(url, testObject))
    behave like aTracingHttpCall("PUT", "PUT", new StubbedHttpPut(defaultHttpResponse)) { _.PUT(url, testObject) }

    "Invoke any hooks provided" in {

      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testPut = new StubbedHttpPut(dummyResponseFuture)
      testPut.PUT(url, testObject).futureValue

      val testJson = Json.stringify(trcreads.writes(testObject))

      verify(testPut.testHook1)(url, "PUT", Some(testJson), dummyResponseFuture)
      verify(testPut.testHook2)(url, "PUT", Some(testJson), dummyResponseFuture)
    }
  }
}
