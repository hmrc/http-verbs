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

package uk.gov.hmrc.play.http

import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HttpVerbs._
import play.twirl.api.Html
import uk.gov.hmrc.play.http.hooks.HttpHook

import scala.concurrent.Future

class HttpGetSpec extends WordSpecLike with Matchers with ScalaFutures with CommonHttpBehaviour with IntegrationPatience with MockitoSugar {

  class StubbedHttpGet(doGetResult: Future[HttpResponse] = defaultHttpResponse) extends HttpGet with ConnectionTracingCapturing {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = doGetResult
  }

  "HttpGet" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testGet = new StubbedHttpGet(Future.successful(response))
      testGet.GET(url).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testGet = new StubbedHttpGet(Future.successful(new DummyHttpResponse(testBody, 200)))
      testGet.GET(url).futureValue should be(an[Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testGet = new StubbedHttpGet(Future.successful(new DummyHttpResponse( """{"foo":"t","bar":10}""", 200)))
      testGet.GET[TestClass](url).futureValue should be(TestClass("t", 10))
    }
    behave like anErrorMappingHttpCall(GET, (url, responseF) => new StubbedHttpGet(responseF).GET(url))
    behave like aTracingHttpCall(GET, "GET", new StubbedHttpGet(defaultHttpResponse)) {
      _.GET(url)
    }

    "Invoke any hooks provided" in {
      import uk.gov.hmrc.play.test.Concurrent.await

      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testGet = new StubbedHttpGet(dummyResponseFuture)
      await(testGet.GET(url))

      verify(testGet.testHook1)(url, "GET", None, dummyResponseFuture)
      verify(testGet.testHook2)(url, "GET", None, dummyResponseFuture)
    }

  }


}
