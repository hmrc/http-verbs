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

import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpGetSpec extends WordSpecLike with Matchers with ScalaFutures with CommonHttpBehaviour with IntegrationPatience with MockitoSugar {

  class StubbedHttpGet(doGetResult: Future[HttpResponse] = defaultHttpResponse) extends HttpGet with ConnectionTracingCapturing {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = doGetResult
  }

  class UrlTestingHttpGet() extends HttpGet {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)
    var lastUrl: Option[String] = None

    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
      lastUrl = Some(url)
      defaultHttpResponse
    }
  }

  "HttpGet" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testGet = new StubbedHttpGet(Future.successful(response))
      testGet.GET(url).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testGet = new StubbedHttpGet(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testGet.GET[TestClass](url).futureValue should be (TestClass("t", 10))
    }
    behave like anErrorMappingHttpCall("GET", (url, responseF) => new StubbedHttpGet(responseF).GET(url))
    behave like aTracingHttpCall("GET", "GET", new StubbedHttpGet(defaultHttpResponse)) { _.GET(url) }

    "Invoke any hooks provided" in {

      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testGet = new StubbedHttpGet(dummyResponseFuture)
      testGet.GET(url).futureValue

      verify(testGet.testHook1)(url, "GET", None, dummyResponseFuture)
      verify(testGet.testHook2)(url, "GET", None, dummyResponseFuture)
    }
  }

  "HttpGet with params Seq" should {
    "return an empty string if the query parameters is empty" in {
      val expected = Some("http://test.net")
      val testGet = new UrlTestingHttpGet()
      testGet.GET("http://test.net", Seq())
      testGet.lastUrl shouldBe expected
    }

    "return a url with a single param pair" in {
      val expected = Some("http://test.net?one=1")
      val testGet = new UrlTestingHttpGet()
      testGet.GET("http://test.net", Seq(("one", "1")))
      testGet.lastUrl shouldBe expected
    }

    "return a url with a multiple param pairs" in {
      val expected = Some("http://test.net?one=1&two=2&three=3")
      val testGet = new UrlTestingHttpGet()
      testGet.GET("http://test.net", Seq(("one", "1"),("two", "2"), ("three", "3")))
      testGet.lastUrl shouldBe expected
    }

    "return a url with encoded param pairs" in {
      val expected = Some("http://test.net?email=test%2Balias%40email.com&data=%7B%22message%22%3A%22in+json+format%22%7D")
      val testGet = new UrlTestingHttpGet()
      testGet.GET("http://test.net", Seq(("email", "test+alias@email.com"),("data", "{\"message\":\"in json format\"}")))
      testGet.lastUrl shouldBe expected
    }

    "return a url with duplicate param pairs" in {
      val expected = Some("http://test.net?one=1&two=2&one=11")
      val testGet = new UrlTestingHttpGet()
      testGet.GET("http://test.net", Seq(("one", "1"),("two", "2"), ("one", "11")))
      testGet.lastUrl shouldBe expected
    }

    "raise an exception if the URL provided already has a query string" in {
      val testGet = new UrlTestingHttpGet()

      a [UrlValidationException] should be thrownBy testGet.GET("http://test.net?should=not=be+here", Seq(("one", "1")))
    }
  }
}
