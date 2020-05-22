/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => is}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpPutSpec extends AnyWordSpecLike with Matchers with CommonHttpBehaviour {
  import ExecutionContext.Implicits.global

  class StubbedHttpPut(doPutResult: Future[HttpResponse])
      extends HttpPut
      with MockitoSugar
      with ConnectionTracingCapturing {
    val testHook1                                   = mock[HttpHook]
    val testHook2                                   = mock[HttpHook]
    val hooks                                       = Seq(testHook1, testHook2)
    override def configuration: Option[Config]      = None
    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPutString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
      doPutResult

    override def doPut[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
      doPutResult
  }

  "HttpPut" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val payload = HttpResponse(200, testBody)
      val response = Future.successful(payload)
      val testPut  = new StubbedHttpPut(response)
      testPut.PUT[TestRequestClass, HttpResponse](url, testObject).futureValue shouldBe payload
    }
    "be able to return objects deserialised from JSON" in {
      val payload = HttpResponse(200, """{"foo":"t","bar":10}""")
      val response = Future.successful(payload)
      val testPut = new StubbedHttpPut(response)
      testPut.PUT[TestRequestClass, TestClass](url, testObject).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall("PUT", (url, responseF) => new StubbedHttpPut(responseF).PUT[TestRequestClass, HttpResponse](url, testObject))
    behave like aTracingHttpCall("PUT", "PUT", new StubbedHttpPut(defaultHttpResponse)) { _.PUT[TestRequestClass, HttpResponse](url, testObject) }

    "be able to pass additional headers on request" in {
      val outcome = HttpResponse(200, testBody)
      val response = Future.successful(outcome)
      val testPut  = new StubbedHttpPut(response)
      testPut.PUT[TestRequestClass, HttpResponse](url, testObject, Seq("If-Match" -> "foobar")).futureValue shouldBe outcome
    }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPut             = new StubbedHttpPut(dummyResponseFuture)
      val testJson            = Json.stringify(trcreads.writes(testObject))

      testPut.PUT[TestRequestClass, HttpResponse](url, testObject).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPut.testHook1).apply(is(url), is("PUT"), is(Some(testJson)), respArgCaptor1.capture())(any(), any())
      verify(testPut.testHook2).apply(is(url), is("PUT"), is(Some(testJson)), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }

  "HttpPut.PUTString" should {
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val eventualResponse = Future.successful(response)
      val testPUT = new StubbedHttpPut(eventualResponse)
      testPUT.PUTString[HttpResponse](url, testRequestBody, Seq.empty).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val eventualResponse = Future.successful(HttpResponse(200, """{"foo":"t","bar":10}"""))
      val testPUT = new StubbedHttpPut(eventualResponse)
      testPUT.PUTString[TestClass](url, testRequestBody, Seq.empty).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(
      "PUT",
      (url, responseF) => new StubbedHttpPut(responseF).PUTString[HttpResponse](url, testRequestBody, Seq.empty))
    behave like aTracingHttpCall("PUT", "PUT", new StubbedHttpPut(defaultHttpResponse)) {
      _.PUTString[HttpResponse](url, testRequestBody, Seq.empty)
    }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, """{"foo":"t","bar":10}""")
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPut            = new StubbedHttpPut(dummyResponseFuture)

      testPut.PUTString[TestClass](url, testRequestBody, Seq.empty).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPut.testHook1)
        .apply(is(url), is("PUT"), is(Some(testRequestBody)), respArgCaptor1.capture())(any(), any())
      verify(testPut.testHook2)
        .apply(is(url), is("PUT"), is(Some(testRequestBody)), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }
}
