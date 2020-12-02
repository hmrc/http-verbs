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
import com.typesafe.config.{Config, ConfigFactory}
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

class HttpPostSpec extends AnyWordSpecLike with Matchers with CommonHttpBehaviour {
  import ExecutionContext.Implicits.global

  class StubbedHttpPost(doPostResult: Future[HttpResponse])
      extends HttpPost
      with MockitoSugar
      with ConnectionTracingCapturing {
    val testHook1                                   = mock[HttpHook]
    val testHook2                                   = mock[HttpHook]
    val hooks                                       = Seq(testHook1, testHook2)
    override val configuration: Config              = ConfigFactory.load()
    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier, ec: ExecutionContext) =
      doPostResult

    override def doFormPost(url: String, body: Map[String, Seq[String]], headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
      doPostResult

    override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
      doPostResult

    override def doEmptyPost[A](url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
      doPostResult
  }

  "HttpPost.POST" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POST[TestRequestClass, HttpResponse](url, testObject).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testPOST.POST[TestRequestClass, TestClass](url, testObject).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POST[TestRequestClass, HttpResponse](url, testObject))
    behave like aTracingHttpCall("POST", "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POST[TestRequestClass, HttpResponse](url, testObject) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPatch           = new StubbedHttpPost(dummyResponseFuture)

      testPatch.POST[TestRequestClass, HttpResponse](url, testObject)

      val testJson = Json.stringify(trcreads.writes(testObject))

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPatch.testHook1).apply(is(url), is("POST"), is(Some(HookData.FromString(testJson))), respArgCaptor1.capture())(any(), any())
      verify(testPatch.testHook2).apply(is(url), is("POST"), is(Some(HookData.FromString(testJson))), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }

  "HttpPost.POSTForm" should {
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTForm[HttpResponse](url, Map()).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testPOST.POSTForm[TestClass](url, Map()).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POSTForm[HttpResponse](url, Map()))
    behave like aTracingHttpCall("POST", "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POSTForm[HttpResponse](url, Map()) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost            = new StubbedHttpPost(dummyResponseFuture)

      testPost.POSTForm[HttpResponse](url, Map()).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPost.testHook1).apply(is(url), is("POST"), is(Some(HookData.FromMap(Map()))), respArgCaptor1.capture())(any(), any())
      verify(testPost.testHook2).apply(is(url), is("POST"), is(Some(HookData.FromMap(Map()))), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }

  "HttpPost.POSTString" should {
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTString[HttpResponse](url, testRequestBody).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testPOST.POSTString[TestClass](url, testRequestBody).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(
      "POST",
      (url, responseF) => new StubbedHttpPost(responseF).POSTString[HttpResponse](url, testRequestBody))
    behave like aTracingHttpCall("POST", "POST", new StubbedHttpPost(defaultHttpResponse)) {
      _.POSTString[HttpResponse](url, testRequestBody)
    }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, """{"foo":"t","bar":10}""")
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost            = new StubbedHttpPost(dummyResponseFuture)

      testPost.POSTString[TestClass](url, testRequestBody).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPost.testHook1)
        .apply(is(url), is("POST"), is(Some(HookData.FromString(testRequestBody))), respArgCaptor1.capture())(any(), any())
      verify(testPost.testHook2)
        .apply(is(url), is("POST"), is(Some(HookData.FromString(testRequestBody))), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }

  "HttpPost.POSTEmpty" should {
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTEmpty[HttpResponse](url).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testPOST.POSTEmpty[TestClass](url).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty[HttpResponse](url))
    behave like aTracingHttpCall("POST", "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POSTEmpty[HttpResponse](url) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, """{"foo":"t","bar":10}""")
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost            = new StubbedHttpPost(dummyResponseFuture)

      testPost.POSTEmpty[TestClass](url).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testPost.testHook1).apply(is(url), is("POST"), is(None), respArgCaptor1.capture())(any(), any())
      verify(testPost.testHook2).apply(is(url), is("POST"), is(None), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }

  "POSTEmpty" should {
    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty[HttpResponse](url))
    behave like aTracingHttpCall[StubbedHttpPost]("POST", "POSTEmpty", new StubbedHttpPost(defaultHttpResponse)) {
      _.POSTEmpty[HttpResponse](url)
    }
  }
}
