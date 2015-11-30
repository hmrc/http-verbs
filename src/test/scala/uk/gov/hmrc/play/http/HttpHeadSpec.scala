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

class HttpHeadSpec extends WordSpecLike with Matchers with ScalaFutures with CommonHttpBehaviour with IntegrationPatience with MockitoSugar {

  class StubbedHttpHead(doHeadResult: Future[HttpResponse] = defaultHttpResponse) extends HttpHead with ConnectionTracingCapturing {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    override def doHead(url: String, precondition: Precondition)(implicit hc: HeaderCarrier): Future[HttpResponse] = doHeadResult
  }

  "HttpHead" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse("", 200)
      val testHead = new StubbedHttpHead(Future.successful(response))
      testHead.HEAD(url).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testHead = new StubbedHttpHead(Future.successful(new DummyHttpResponse("", 200)))
      private val entity = testHead.HEAD(url).futureValue
      entity should be(an[Html])
      entity.body should be("")
    }
    "be able not to deserialise JSON" ignore { // FIXME
      val testGet = new StubbedHttpHead(Future.successful(new DummyHttpResponse("", 200)))
      testGet.HEAD[Option[TestClass]](url).futureValue should be(None)
    }
    behave like anErrorMappingHttpCall(HEAD, (url, responseF) => new StubbedHttpHead(responseF).HEAD(url))
    behave like aTracingHttpCall(HEAD, "HEAD", new StubbedHttpHead(defaultHttpResponse)) {
      _.HEAD(url)
    }

    "Invoke any hooks provided" in {
      import uk.gov.hmrc.play.test.Concurrent.await

      val dummyResponseFuture = Future.successful(new DummyHttpResponse("", 200))
      val testHead = new StubbedHttpHead(dummyResponseFuture)
      await(testHead.HEAD(url))

      verify(testHead.testHook1)(url, "HEAD", None, dummyResponseFuture)
      verify(testHead.testHook2)(url, "HEAD", None, dummyResponseFuture)
    }

  }


}
