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
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpPatchSpec extends AnyWordSpecLike with Matchers with CommonHttpBehaviour {
  import ExecutionContext.Implicits.global

  class StubbedHttpPatch(doPatchResult: Future[HttpResponse], doPatchWithHeaderResult: Future[HttpResponse])
      extends HttpPatch
      with ConnectionTracingCapturing
      with MockitoSugar {
    val testHook1                                   = mock[HttpHook]
    val testHook2                                   = mock[HttpHook]
    val hooks                                       = Seq(testHook1, testHook2)
    override def configuration: Option[Config]      = None
    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")

    def doPatch[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier, ec: ExecutionContext) =
      doPatchResult
  }

  "HttpPatch" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response  = HttpResponse(200, testBody)
      val testPatch = new StubbedHttpPatch(Future.successful(response), Future.successful(response))
      testPatch.PATCH[TestRequestClass, HttpResponse](url, testObject, Seq("header" -> "foo")).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val response= Future.successful(HttpResponse(200, """{"foo":"t","bar":10}"""))
      val testPatch = new StubbedHttpPatch(response, response)
      testPatch.PATCH[TestRequestClass, TestClass](url, testObject, Seq("header" -> "foo")).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(
      "PATCH",
      (url, responseF) => new StubbedHttpPatch(responseF, responseF).PATCH[TestRequestClass, HttpResponse](url, testObject, Seq("header" -> "foo")))
    behave like aTracingHttpCall("PATCH", "PATCH", new StubbedHttpPatch(defaultHttpResponse, defaultHttpResponse)) {
      _.PATCH[TestRequestClass, HttpResponse](url, testObject, Seq("header" -> "foo"))
    }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPatch           = new StubbedHttpPatch(dummyResponseFuture, dummyResponseFuture)
      val testJson            = Json.stringify(trcreads.writes(testObject))

      testPatch.PATCH[TestRequestClass, HttpResponse](url, testObject, Seq("header" -> "foo")).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPatch.testHook1)
        .apply(is(url), is("PATCH"), is(Some(HookData.FromString(testJson))), respArgCaptor1.capture())(any(), any())
      verify(testPatch.testHook2)
        .apply(is(url), is("PATCH"), is(Some(HookData.FromString(testJson))), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }
}
