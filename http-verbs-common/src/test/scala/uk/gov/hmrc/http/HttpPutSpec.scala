/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.hooks.{Data, HookData, HttpHook, RequestData, ResponseData}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpPutSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with CommonHttpBehaviour {
  import ExecutionContext.Implicits.global

  class StubbedHttpPut(
    doPutResult: Future[HttpResponse]
  ) extends HttpPut
       with ConnectionTracingCapturing {

    val testHook1: HttpHook                         = mock[HttpHook](withSettings.lenient)
    val testHook2: HttpHook                         = mock[HttpHook](withSettings.lenient)
    val hooks                                       = Seq(testHook1, testHook2)
    override val configuration: Config              = ConfigFactory.load()
    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPutString(
      url: String,
      body: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      doPutResult

    override def doPut[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] =
      doPutResult
  }

  class UrlTestingHttpPut() extends HttpPut with PutHttpTransport {
    var lastUrl: Option[String] = None

    override val configuration: Config = ConfigFactory.load()

    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPutString(
      url: String,
      body: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override def doPut[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override val hooks: Seq[HttpHook] = Seq.empty
  }

  "HttpPut" should {
    val testObject = TestRequestClass("a", 1)

    "return plain responses" in {
      val payload = HttpResponse(200, testBody)
      val response = Future.successful(payload)
      val testPut  = new StubbedHttpPut(response)
      testPut.PUT[TestRequestClass, HttpResponse](url, testObject).futureValue shouldBe payload
    }

    "return objects deserialised from JSON" in {
      val payload = HttpResponse(200, """{"foo":"t","bar":10}""")
      val response = Future.successful(payload)
      val testPut = new StubbedHttpPut(response)
      testPut.PUT[TestRequestClass, TestClass](url, testObject).futureValue should be(TestClass("t", 10))
    }

    "return a url with encoded param pairs with url builder" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com&data=%7B%22message%22:%22in+json+format%22%7D")
      val testPut = new UrlTestingHttpPut()
      val queryParams = Seq("email" -> "test+alias@email.com", "data" -> "{\"message\":\"in json format\"}")
      testPut.PUT[TestRequestClass, HttpResponse](url"http://test.net?$queryParams", testObject)
      testPut.lastUrl shouldBe expected
    }

    "return an encoded url when query param is in baseUrl" in {
      val expected =
        Some("http://test.net?email=testalias@email.com&foo=bar&data=%7B%22message%22:%22in+json+format%22%7D")
      val testPut = new UrlTestingHttpPut()
      val queryParams = Seq("data" -> "{\"message\":\"in json format\"}")
      testPut
        .PUT[TestRequestClass, HttpResponse](url"http://test.net?email=testalias@email.com&foo=bar&$queryParams", testObject)
      testPut.lastUrl shouldBe expected
    }

    "return encoded url when query params are already encoded" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com")
      val testPut = new UrlTestingHttpPut()
      testPut
        .PUTString[HttpResponse](url"http://test.net?email=test%2Balias@email.com", "some-string")
      testPut.lastUrl shouldBe expected
    }

    "return encoded url when path needs encoding" in {
      val expected =
        Some("http://test.net/some%2Fother%2Froute%3Fa=b&c=d%23/something?email=testalias@email.com")
      val testPut = new UrlTestingHttpPut()
      val paths = List("some/other/route?a=b&c=d#", "something")
      val email = "testalias@email.com"
      testPut.PUTString[HttpResponse](url"http://test.net/$paths?email=$email", "some-string")
      testPut.lastUrl shouldBe expected
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

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testPut.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url)

      verify(testPut.testHook1).apply(eqTo("PUT"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testPut.testHook2).apply(eqTo("PUT"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

      val request1 = requestCaptor1.value
      request1.headers  should contain allElementsOf(headers)
      request1.body     shouldBe Some(Data.pure(HookData.FromString(testJson)))

      val request2 = requestCaptor2.value
      request2.headers  should contain allElementsOf(headers)
      request2.body     shouldBe Some(Data.pure(HookData.FromString(testJson)))

      // verifying directly without ArgCaptor doesn't work since Futures are different instances
      // e.g. Future.successful(5) != Future.successful(5)
      val response1 = responseFCaptor1.value.futureValue
      response1.status shouldBe 200
      response1.body shouldBe Data.pure(testBody)

      val response2 = responseFCaptor2.value.futureValue
      response2.status shouldBe 200
      response2.body shouldBe Data.pure(testBody)
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
      (url, responseF) => new StubbedHttpPut(responseF).PUTString[HttpResponse](url, testRequestBody, Seq.empty)
    )
    behave like aTracingHttpCall("PUT", "PUT", new StubbedHttpPut(defaultHttpResponse)) {
      _.PUTString[HttpResponse](url, testRequestBody, Seq.empty)
    }

    "Invoke any hooks provided" in {
      val testBody            = """{"foo":"t","bar":10}"""
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPut            = new StubbedHttpPut(dummyResponseFuture)

      testPut.PUTString[TestClass](url, testRequestBody, Seq.empty).futureValue

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testPut.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url)

      verify(testPut.testHook1).apply(eqTo("PUT"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testPut.testHook2).apply(eqTo("PUT"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

      val request1 = requestCaptor1.value
      request1.headers  should contain allElementsOf(headers)
      request1.body     shouldBe Some(Data.pure(HookData.FromString(testRequestBody)))

      val request2 = requestCaptor2.value
      request2.headers  should contain allElementsOf(headers)
      request2.body     shouldBe Some(Data.pure(HookData.FromString(testRequestBody)))

      // verifying directly without ArgCaptor doesn't work since Futures are different instances
      // e.g. Future.successful(5) != Future.successful(5)
      val response1 = responseFCaptor1.value.futureValue
      response1.status shouldBe 200
      response1.body shouldBe Data.pure(testBody)

      val response2 = responseFCaptor2.value.futureValue
      response2.status shouldBe 200
      response2.body shouldBe Data.pure(testBody)
    }
  }
}
