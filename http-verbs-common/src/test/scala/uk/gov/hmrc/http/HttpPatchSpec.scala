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
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpPatchSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with CommonHttpBehaviour {
  import ExecutionContext.Implicits.global

  class StubbedHttpPatch(
    doPatchResult          : Future[HttpResponse],
    doPatchWithHeaderResult: Future[HttpResponse]
  ) extends HttpPatch
       with ConnectionTracingCapturing {

    val testHook1: HttpHook                         = mock[HttpHook](withSettings.lenient)
    val testHook2: HttpHook                         = mock[HttpHook](withSettings.lenient)
    val hooks                                       = Seq(testHook1, testHook2)
    override val configuration: Config              = ConfigFactory.load()
    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPatch[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] =
      doPatchResult
  }

  class UrlTestingHttpPatch() extends HttpPatch with PatchHttpTransport {

    var lastUrl: Option[String] = None

    override val configuration: Config = ConfigFactory.load()

    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doPatch[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit rds: Writes[A],
        ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override val hooks: Seq[HttpHook] = Seq()

  }

  "HttpPatch" should {
    val testObject = TestRequestClass("a", 1)

    "return plain responses" in {
      val response  = HttpResponse(200, testBody)
      val testPatch = new StubbedHttpPatch(Future.successful(response), Future.successful(response))
      testPatch.PATCH[TestRequestClass, HttpResponse](url, testObject, Seq("header" -> "foo")).futureValue shouldBe response
    }

    "return objects deserialised from JSON" in {
      val response= Future.successful(HttpResponse(200, """{"foo":"t","bar":10}"""))
      val testPatch = new StubbedHttpPatch(response, response)
      testPatch.PATCH[TestRequestClass, TestClass](url, testObject, Seq("header" -> "foo")).futureValue should be(TestClass("t", 10))
    }

    "return a url with encoded param pairs with url builder" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com&data=%7B%22message%22:%22in+json+format%22%7D")
      val testPatch = new UrlTestingHttpPatch()
      val queryParams = Seq("email" -> "test+alias@email.com", "data" -> "{\"message\":\"in json format\"}")
      testPatch.PATCH[TestRequestClass, HttpResponse](url"http://test.net?$queryParams", testObject)
      testPatch.lastUrl shouldBe expected
    }

    "return an encoded url when query param is in baseUrl" in {
      val expected =
        Some("http://test.net?email=testalias@email.com&foo=bar&data=%7B%22message%22:%22in+json+format%22%7D")
      val testPatch = new UrlTestingHttpPatch()
      val queryParams = Seq("data" -> "{\"message\":\"in json format\"}")
      testPatch
        .PATCH[TestRequestClass, HttpResponse](url"http://test.net?email=testalias@email.com&foo=bar&$queryParams", testObject)
      testPatch.lastUrl shouldBe expected
    }

    "return encoded url when query params are already encoded" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com")
      val testPatch = new UrlTestingHttpPatch()
      testPatch
        .PATCH[TestRequestClass, HttpResponse](url"http://test.net?email=test%2Balias@email.com", testObject)
      testPatch.lastUrl shouldBe expected
    }

    "return encoded url when path needs encoding" in {
      val expected =
        Some("http://test.net/some%2Fother%2Froute%3Fa=b&c=d%23/something?email=testalias@email.com")
      val testPatch = new UrlTestingHttpPatch()
      val paths = List("some/other/route?a=b&c=d#", "something")
      val email = "testalias@email.com"
      testPatch.PATCH[TestRequestClass, HttpResponse](url"http://test.net/$paths?email=$email", testObject)
      testPatch.lastUrl shouldBe expected
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

      val respArgCaptor1 = ArgCaptor[Future[HttpResponse]]
      val respArgCaptor2 = ArgCaptor[Future[HttpResponse]]

      val headerCaptor1 = ArgCaptor[Seq[(String, String)]]
      val headerCaptor2 = ArgCaptor[Seq[(String, String)]]

      val config = HeaderCarrier.Config.fromConfig(testPatch.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url, Seq("header" -> "foo"))

      verify(testPatch.testHook1).apply(eqTo("PATCH"), eqTo(url"$url"), headerCaptor1, eqTo(Some(HookData.FromString(testJson))), respArgCaptor1)(any, any)
      verify(testPatch.testHook2).apply(eqTo("PATCH"), eqTo(url"$url"), headerCaptor2, eqTo(Some(HookData.FromString(testJson))), respArgCaptor2)(any, any)

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.value.futureValue shouldBe dummyResponse
      respArgCaptor2.value.futureValue shouldBe dummyResponse

      headerCaptor1.value should contain allElementsOf(headers)
      headerCaptor2.value should contain allElementsOf(headers)
    }
  }
}
