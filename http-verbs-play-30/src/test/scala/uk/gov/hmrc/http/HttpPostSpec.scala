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

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import org.mockito.{ArgumentMatchersSugar, Strictness}
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.hooks.{Data, HookData, HttpHook, RequestData, ResponseData}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpPostSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with CommonHttpBehaviour {
  import ExecutionContext.Implicits.global

  class StubbedHttpPost(
    doPostResult: Future[HttpResponse]
  ) extends HttpPost
       with ConnectionTracingCapturing {

    val testHook1: HttpHook                         = mock[HttpHook](withSettings.strictness(Strictness.Lenient))
    val testHook2: HttpHook                         = mock[HttpHook](withSettings.strictness(Strictness.Lenient))
    val hooks                                       = Seq(testHook1, testHook2)
    override val configuration: Config              = ConfigFactory.load()
    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPost[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] =
      doPostResult

    override def doFormPost(
      url: String,
      body: Map[String, Seq[String]],
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      doPostResult

    override def doPostString(
      url: String,
      body: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      doPostResult

    override def doEmptyPost[A](
      url: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      doPostResult
  }

  class UrlTestingHttpPost()
    extends HttpPost
      with PostHttpTransport {

    var lastUrl: Option[String] = None

    override val configuration: Config              = ConfigFactory.load()

    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPost[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override def doFormPost(
      url: String,
      body: Map[String, Seq[String]],
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override def doPostString(
      url: String,
      body: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override def doEmptyPost[A](
      url: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override val hooks: Seq[HttpHook] = Seq.empty
  }

  "HttpPost.POST" should {
    val testObject = TestRequestClass("a", 1)

    "return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POST[TestRequestClass, HttpResponse](url, testObject).futureValue shouldBe response
    }

    "return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testPOST.POST[TestRequestClass, TestClass](url, testObject).futureValue should be(TestClass("t", 10))
    }

    "return a url with encoded param pairs with url builder" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com&data=%7B%22message%22:%22in+json+format%22%7D")
      val testPost = new UrlTestingHttpPost()
      val queryParams = Seq("email" -> "test+alias@email.com", "data" -> "{\"message\":\"in json format\"}")
      testPost.POST[TestRequestClass, HttpResponse](url"http://test.net?$queryParams", testObject)
      testPost.lastUrl shouldBe expected
    }

    "return an encoded url when query param is in baseUrl" in {
      val expected =
        Some("http://test.net?email=testalias@email.com&foo=bar&data=%7B%22message%22:%22in+json+format%22%7D")
      val testPost = new UrlTestingHttpPost()
      val queryParams = Seq("data" -> "{\"message\":\"in json format\"}")
      testPost
        .POSTForm[HttpResponse](url"http://test.net?email=testalias@email.com&foo=bar&$queryParams", Map.empty[String, Seq[String]])
      testPost.lastUrl shouldBe expected
    }

    "return encoded url when query params are already encoded" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com")
      val testPost = new UrlTestingHttpPost()
      testPost
        .POSTString[HttpResponse](url"http://test.net?email=test%2Balias@email.com", "post body")
      testPost.lastUrl shouldBe expected
    }

    "return encoded url when path needs encoding" in {
      val expected =
        Some("http://test.net/some%2Fother%2Froute%3Fa=b&c=d%23/something?email=testalias@email.com")
      val testPost = new UrlTestingHttpPost()
      val paths = List("some/other/route?a=b&c=d#", "something")
      val email = "testalias@email.com"
      testPost.POSTEmpty[HttpResponse](url"http://test.net/$paths?email=$email")
      testPost.lastUrl shouldBe expected
    }

    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POST[TestRequestClass, HttpResponse](url, testObject))
    behave like aTracingHttpCall("POST", "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POST[TestRequestClass, HttpResponse](url, testObject) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost           = new StubbedHttpPost(dummyResponseFuture)

      testPost.POST[TestRequestClass, HttpResponse](url, testObject)

      val testJson = Json.stringify(trcreads.writes(testObject))

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testPost.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url)

      verify(testPost.testHook1).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testPost.testHook2).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

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

  "HttpPost.POSTForm" should {
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTForm[HttpResponse](url, Map.empty[String, Seq[String]], Seq.empty).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testPOST.POSTForm[TestClass](url, Map.empty[String, Seq[String]], Seq.empty).futureValue should be(TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POSTForm[HttpResponse](url, Map.empty[String, Seq[String]], Seq.empty))
    behave like aTracingHttpCall("POST", "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POSTForm[HttpResponse](url, Map.empty[String, Seq[String]], Seq.empty) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost            = new StubbedHttpPost(dummyResponseFuture)

      testPost.POSTForm[HttpResponse](url, Map.empty[String, Seq[String]], Seq.empty).futureValue

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testPost.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url)

      verify(testPost.testHook1).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testPost.testHook2).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

      val request1 = requestCaptor1.value
      request1.headers  should contain allElementsOf(headers)
      request1.body     shouldBe Some(Data.pure(HookData.FromMap(Map())))

      val request2 = requestCaptor2.value
      request2.headers  should contain allElementsOf(headers)
      request2.body     shouldBe Some(Data.pure(HookData.FromMap(Map())))

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
      val testBody            = """{"foo":"t","bar":10}"""
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost            = new StubbedHttpPost(dummyResponseFuture)

      testPost.POSTString[TestClass](url, testRequestBody).futureValue

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testPost.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url)

      verify(testPost.testHook1).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testPost.testHook2).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

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
      val testBody            = """{"foo":"t","bar":10}"""
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testPost            = new StubbedHttpPost(dummyResponseFuture)

      testPost.POSTEmpty[TestClass](url).futureValue

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testPost.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url)

      verify(testPost.testHook1).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testPost.testHook2).apply(eqTo("POST"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

      val request1 = requestCaptor1.value
      request1.headers  should contain allElementsOf(headers)
      request1.body     shouldBe None

      val request2 = requestCaptor2.value
      request2.headers  should contain allElementsOf(headers)
      request2.body     shouldBe None

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

  "POSTEmpty" should {
    behave like anErrorMappingHttpCall("POST", (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty[HttpResponse](url))
    behave like aTracingHttpCall[StubbedHttpPost]("POST", "POSTEmpty", new StubbedHttpPost(defaultHttpResponse)) {
      _.POSTEmpty[HttpResponse](url)
    }
  }
}
