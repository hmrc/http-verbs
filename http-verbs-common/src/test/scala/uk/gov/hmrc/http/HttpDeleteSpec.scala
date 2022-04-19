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
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.hooks.{Body, HttpHook, RequestData, ResponseData}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpDeleteSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with CommonHttpBehaviour {

  import ExecutionContext.Implicits.global

  class StubbedHttpDelete(
    doDeleteResult: Future[HttpResponse]
  ) extends HttpDelete
       with DeleteHttpTransport
       with ConnectionTracingCapturing {

    val testHook1: HttpHook                         = mock[HttpHook](withSettings.lenient)
    val testHook2: HttpHook                         = mock[HttpHook](withSettings.lenient)
    val hooks                                       = Seq(testHook1, testHook2)
    override val configuration: Config              = ConfigFactory.load()
    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    def appName: String = ???

    override def doDelete(
      url: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      doDeleteResult
  }

  class UrlTestingHttpDelete() extends HttpDelete with DeleteHttpTransport {

    var lastUrl: Option[String] = None

    override val configuration: Config = ConfigFactory.load()

    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doDelete(
      url: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }

    override val hooks: Seq[HttpHook] = Seq()
  }

  "HttpDelete" should {

    "return plain responses" in {
      val response   = HttpResponse(200, testBody)
      val testDelete = new StubbedHttpDelete(Future.successful(response))
      testDelete.DELETE[HttpResponse](url, Seq("foo" -> "bar")).futureValue shouldBe response
    }

    "return objects deserialised from JSON" in {
      val testDelete = new StubbedHttpDelete(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testDelete
        .DELETE[TestClass](url, Seq("foo" -> "bar"))
        .futureValue(Timeout(Span(2, Seconds)), Interval(Span(15, Millis))) shouldBe TestClass("t", 10)
    }

    "return a url with encoded param pairs with url builder" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com&data=%7B%22message%22:%22in+json+format%22%7D")
      val testDelete = new UrlTestingHttpDelete()
      val queryParams = Seq("email" -> "test+alias@email.com", "data" -> "{\"message\":\"in json format\"}")
      testDelete.DELETE[HttpResponse](url"http://test.net?$queryParams")
      testDelete.lastUrl shouldBe expected
    }

    "return an encoded url when query param is in baseUrl" in {
      val expected =
        Some("http://test.net?email=testalias@email.com&foo=bar&data=%7B%22message%22:%22in+json+format%22%7D")
      val testDelete = new UrlTestingHttpDelete()
      val queryParams = Seq("data" -> "{\"message\":\"in json format\"}")
      testDelete
        .DELETE[HttpResponse](url"http://test.net?email=testalias@email.com&foo=bar&$queryParams")
      testDelete.lastUrl shouldBe expected
    }

    "return encoded url when query params are already encoded" in {
      val expected =
        Some("http://test.net?email=test%2Balias@email.com")
      val testDelete = new UrlTestingHttpDelete()
      testDelete
        .DELETE[HttpResponse](url"http://test.net?email=test%2Balias@email.com")
      testDelete.lastUrl shouldBe expected
    }

    "return encoded url when path needs encoding" in {
      val expected =
        Some("http://test.net/some%2Fother%2Froute%3Fa=b&c=d%23/something?email=testalias@email.com")
      val testDelete = new UrlTestingHttpDelete()
      val paths = List("some/other/route?a=b&c=d#", "something")
      val email = "testalias@email.com"
      testDelete.DELETE[HttpResponse](url"http://test.net/$paths?email=$email")
      testDelete.lastUrl shouldBe expected
    }

    behave like anErrorMappingHttpCall("DELETE", (url, responseF) => new StubbedHttpDelete(responseF).DELETE[HttpResponse](url, Seq("foo" -> "bar")))
    behave like aTracingHttpCall("DELETE", "DELETE", new StubbedHttpDelete(defaultHttpResponse)) { _.DELETE[HttpResponse](url, Seq("foo" -> "bar")) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testDelete          = new StubbedHttpDelete(dummyResponseFuture)

      testDelete.DELETE[HttpResponse](url, Seq("header" -> "foo")).futureValue

      val responseFCaptor1 = ArgCaptor[Future[ResponseData]]
      val responseFCaptor2 = ArgCaptor[Future[ResponseData]]

      val requestCaptor1 = ArgCaptor[RequestData]
      val requestCaptor2 = ArgCaptor[RequestData]

      val config = HeaderCarrier.Config.fromConfig(testDelete.configuration)
      val headers = HeaderCarrier.headersForUrl(config, url, Seq("header" -> "foo"))

      verify(testDelete.testHook1).apply(eqTo("DELETE"), eqTo(url"$url"), requestCaptor1, responseFCaptor1)(any, any)
      verify(testDelete.testHook2).apply(eqTo("DELETE"), eqTo(url"$url"), requestCaptor2, responseFCaptor2)(any, any)

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
      response1.body shouldBe Body.Complete(testBody)

      val response2 = responseFCaptor2.value.futureValue
      response2.status shouldBe 200
      response2.body shouldBe Body.Complete(testBody)
    }
  }
}
