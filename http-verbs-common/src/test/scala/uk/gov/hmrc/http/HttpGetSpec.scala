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

package uk.gov.hmrc.http

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => is}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpGetSpec
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with CommonHttpBehaviour
    with IntegrationPatience
    with MockitoSugar {

  import ExecutionContext.Implicits.global

  class StubbedHttpGet(doGetResult: Future[HttpResponse] = defaultHttpResponse)
    extends HttpGet
      with ConnectionTracingCapturing {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    override def configuration: Option[Config] = None

    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doGet(
      url: String,
      headers: Seq[(String, String)])(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] =
      doGetResult
  }

  class UrlTestingHttpGet() extends HttpGet with GetHttpTransport {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)
    var lastUrl: Option[String] = None

    override def configuration: Option[Config] = None

    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")

    override def doGet(
      url: String,
      headers: Seq[(String, String)])(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }
  }

  "HttpGet" should {
    "be able to return plain responses" in {
      val response = HttpResponse(200, testBody)
      val testGet = new StubbedHttpGet(Future.successful(response))
      testGet.GET[HttpResponse](url, Seq("header" -> "foo")).futureValue shouldBe response
    }

    "be able to return objects deserialised from JSON" in {
      val testGet = new StubbedHttpGet(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testGet.GET[TestClass](url, Seq("header" -> "foo")).futureValue should be(TestClass("t", 10))
    }

    "be able to return Some[T] when deserialising as an option of object from JSON" in {
      val testGet = new StubbedHttpGet(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testGet.GET[Option[TestClass]](url, Seq("header" -> "foo")).futureValue should be(Some(TestClass("t", 10)))
    }

    // By adding an Option to your case class, the 404 is translated into None
    "return None when 404 returned for GET as an option of object" in {
      val testGet = new StubbedHttpGet(Future.successful(HttpResponse(404, "This is an expected Not Found")))
      testGet.GET[Option[TestClass]](url, Seq("header" -> "foo")).futureValue should be(None)
    }

    "throw expected exception when JSON deserialisation fails for option of an object" in {
      val testGet = new StubbedHttpGet(Future.successful(HttpResponse(200, "Not JSON")))
      an[Exception] shouldBe thrownBy {
        testGet.GET[Option[TestClass]](url, Seq("header" -> "foo")).futureValue
      }
    }

    behave like anErrorMappingHttpCall(
      "GET",
      (url, responseF) => new StubbedHttpGet(responseF).GET[HttpResponse](url, Seq("header" -> "foo")))
    behave like aTracingHttpCall("GET", "GET", new StubbedHttpGet(defaultHttpResponse)) {
      _.GET[HttpResponse](url, Seq("header" -> "foo"))
    }

    "Invoke any hooks provided" in {
      val dummyResponse = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val testGet = new StubbedHttpGet(dummyResponseFuture)

      testGet.GET[HttpResponse](url).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testGet.testHook1).apply(is(url), is("GET"), is(None), respArgCaptor1.capture())(any(), any())
      verify(testGet.testHook2).apply(is(url), is("GET"), is(None), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }

  "HttpGet with params Seq" should {
    "return an empty string if the query parameters is empty" in {
      val expected = Some("http://test.net")
      val testGet = new UrlTestingHttpGet()
      testGet.GET[HttpResponse]("http://test.net", Seq())
      testGet.lastUrl shouldBe expected
    }

    "return a url with a single param pair" in {
      val expected = Some("http://test.net?one=1")
      val testGet = new UrlTestingHttpGet()
      testGet.GET[HttpResponse]("http://test.net", Seq(("one", "1")))
      testGet.lastUrl shouldBe expected
    }

    "return a url with a multiple param pairs" in {
      val expected = Some("http://test.net?one=1&two=2&three=3")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse]("http://test.net", Seq(("one", "1"), ("two", "2"), ("three", "3")))
      testGet.lastUrl shouldBe expected
    }

    "return a url with encoded param pairs" in {
      val expected =
        Some("http://test.net?email=test%2Balias%40email.com&data=%7B%22message%22%3A%22in+json+format%22%7D")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse](
          "http://test.net",
          Seq(("email", "test+alias@email.com"), ("data", "{\"message\":\"in json format\"}")))
      testGet.lastUrl shouldBe expected
    }

    "return a url with encoded param pairs with url builder" in {
      val expected =
        Some("http://test.net?email=test%2Balias%40email.com&data=%7B%22message%22%3A%22in+json+format%22%7D")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse](UrlBuilder("http://test.net").addQueryParams(Seq("email" -> "test+alias@email.com", "data" -> "{\"message\":\"in json format\"}")), Seq.empty)
      testGet.lastUrl shouldBe expected
    }

    "return an encoded url when query param is in baseUrl" in {
      val expected =
        Some("http://test.net?email=testalias%40email.com&foo=bar&data=%7B%22message%22%3A%22in+json+format%22%7D")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse](UrlBuilder("http://test.net?email=testalias@email.com&foo=bar").addQueryParams(Seq("data" -> "{\"message\":\"in json format\"}")), Seq.empty)
      testGet.lastUrl shouldBe expected
    }

    "return encoded url when query params are already encoded" in {
      val expected =
        Some("http://test.net?email=testalias%40email.com")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse](UrlBuilder("http://test.net?email=testalias%40email.com"), Seq.empty)
      testGet.lastUrl shouldBe expected
    }

    "return encoded url when path needs encoding" in {
      val expected =
        Some("http://test.net/some%2Fother%2Froute%3Fa=b&c=d%23/something?email=testalias%40email.com")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse](UrlBuilder("http://test.net")
          .addPath("some/other/route?a=b&c=d#")
          .addPath("something")
          .addQueryParam("email" -> "testalias@email.com"), Seq.empty)
      testGet.lastUrl shouldBe expected
    }

    "return a url with duplicate param pairs" in {
      val expected = Some("http://test.net?one=1&two=2&one=11")
      val testGet = new UrlTestingHttpGet()
      testGet
        .GET[HttpResponse]("http://test.net", Seq(("one", "1"), ("two", "2"), ("one", "11")))
      testGet.lastUrl shouldBe expected
    }

    "be able to return plain responses provided already has Query and Header String" in {
      val response = HttpResponse(200, testBody)
      val testGet = new StubbedHttpGet(Future.successful(response))
      testGet.GET[HttpResponse](url, Seq(("one", "1")), Seq("header" -> "foo")).futureValue shouldBe response
    }
  }
}