/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http

import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json.Writes
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.test.Concurrent.await
import uk.gov.hmrc.play.test.Concurrent.liftFuture
import scala.concurrent.Future

class HttpPostSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  class StubbedHttpPost(doPostResult: Future[HttpResponse]) extends HttpPost with ConnectionTracingCapturing with MockAuditing {
    def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier) = doPostResult
    def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = doPostResult
    def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = doPostResult
    def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = doPostResult
    def doPostAndRetrieveStream[I](url: String, body: I, headers: Seq[(String, String)])(implicit wts: Writes[I], hc: HeaderCarrier): Future[StreamingHttpResponse] = ???
  }

  class StubbedStreamingHttpPost(doPostResult: Future[StreamingHttpResponse]) extends HttpPost with ConnectionTracingCapturing with MockAuditing {
    def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier) = ???
    def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = ???
    def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = ???
    def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = ???
    def doPostAndRetrieveStream[I](url: String, body: I, headers: Seq[(String, String)])(implicit wts: Writes[I], hc: HeaderCarrier): Future[StreamingHttpResponse] = doPostResult
  }

  "HttpPost.POSTAndRetrieveStream" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return streamed responses" in {
      val response: DummyStreamingHttpResponse = new DummyStreamingHttpResponse(testBody, 200)
      val testPOST: StubbedStreamingHttpPost = new StubbedStreamingHttpPost(Future.successful(response))
      testPOST.POSTAndRetrieveStream(url, testObject).futureValue shouldBe response
    }

    behave like anErrorMappingStreamingHttpCall(POST, (url, responseF) => new StubbedStreamingHttpPost(responseF).POSTAndRetrieveStream(url, testObject))
    behave like aTracingHttpCall(POST, "POST", new StubbedStreamingHttpPost(defaultStreamingHttpResponse)) { _.POSTAndRetrieveStream(url, testObject) }
  }


  "HttpPost.POST" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POST(url, testObject).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse(testBody, 200)))
      testPOST.POST(url, testObject).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPOST.POST[TestRequestClass, TestClass](url, testObject).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POST(url, testObject))
    behave like aTracingHttpCall(POST, "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POST(url, testObject) }
  }

  "HttpPost.POSTForm" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTForm(url, Map()).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse(testBody, 200)))
      testPOST.POSTForm(url, Map()).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPOST.POSTForm[TestClass](url, Map()).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTForm(url, Map()))
    behave like aTracingHttpCall(POST, "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POSTForm(url, Map()) }
  }

  "HttpPost.POSTString" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTString(url, testRequestBody).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse(testBody, 200)))
      testPOST.POSTString(url, testRequestBody).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPOST.POSTString[TestClass](url, testRequestBody).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTString(url, testRequestBody))
    behave like aTracingHttpCall(POST, "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POSTString(url, testRequestBody) }
  }

  "HttpPost.POSTEmpty" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPOST = new StubbedHttpPost(Future.successful(response))
      testPOST.POSTEmpty(url).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse(testBody, 200)))
      testPOST.POSTEmpty(url).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testPOST = new StubbedHttpPost(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testPOST.POSTEmpty[TestClass](url).futureValue should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty(url))
    behave like aTracingHttpCall(POST, "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POSTEmpty(url) }
  }

  "POSTEmpty"  should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty(url))
    behave like aTracingHttpCall[StubbedHttpPost](POST, "POSTEmpty", new StubbedHttpPost(defaultHttpResponse)) { _.POSTEmpty(url) }
  }

}
