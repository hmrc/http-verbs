/*
 * Copyright 2016 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json.{Json, Writes}
import play.twirl.api.Html
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.test.FileBytes
import scala.concurrent.Future
import uk.gov.hmrc.play.test.Concurrent.await

class HttpPostSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  class StubbedHttpPost(doPostResult: Future[HttpResponse]) extends HttpPost with MockitoSugar with ConnectionTracingCapturing {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier) = doPostResult
    def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = doPostResult
    def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = doPostResult
    def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = doPostResult
    def doBinaryPost(url: String, body: Array[Byte])(implicit hc: HeaderCarrier): Future[HttpResponse] = doPostResult
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

    "Invoke any hooks provided" in {
      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testPatch = new StubbedHttpPost(dummyResponseFuture)
      await(testPatch.POST(url, testObject))

      val testJson = Json.stringify(trcreads.writes(testObject))

      verify(testPatch.testHook1)(url, "POST", Some(testJson), dummyResponseFuture)
      verify(testPatch.testHook2)(url, "POST", Some(testJson), dummyResponseFuture)
    }
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

    "Invoke any hooks provided" in {
      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testPost = new StubbedHttpPost(dummyResponseFuture)
      await(testPost.POSTForm(url, Map()))

      verify(testPost.testHook1)(url, "POST", Some(Map()), dummyResponseFuture)
      verify(testPost.testHook2)(url, "POST", Some(Map()), dummyResponseFuture)
    }
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

    "Invoke any hooks provided" in {
      val dummyResponseFuture = Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200))
      val testPost = new StubbedHttpPost(dummyResponseFuture)
      await(testPost.POSTString[TestClass](url, testRequestBody))

      verify(testPost.testHook1)(url, "POST", Some(testRequestBody), dummyResponseFuture)
      verify(testPost.testHook2)(url, "POST", Some(testRequestBody), dummyResponseFuture)
    }
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

    "Invoke any hooks provided" in {
      val dummyResponseFuture = Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200))
      val testPost = new StubbedHttpPost(dummyResponseFuture)
      await(testPost.POSTEmpty[TestClass](url))

      verify(testPost.testHook1)(url, "POST", None, dummyResponseFuture)
      verify(testPost.testHook2)(url, "POST", None, dummyResponseFuture)
    }
  }

  "POSTEmpty"  should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty(url))
    behave like aTracingHttpCall[StubbedHttpPost](POST, "POSTEmpty", new StubbedHttpPost(defaultHttpResponse)) { _.POSTEmpty(url) }
  }


  "HttpPost.POSTBinary" should {

    "be able to return empty body response responses" in {
      val response = new DummyHttpResponse("", 200)
      val testPOST = new StubbedHttpPost(Future.successful(response))

      testPOST.POSTBinary(url, FileBytes("/testfile.txt")).futureValue shouldBe response
    }

    "be able to return JSON responses" in {
      val response = new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)
      val testPOST = new StubbedHttpPost(Future.successful(response))

      val actualResponse = testPOST.POSTBinary[TestClass](url, FileBytes("/testfile.txt")).futureValue

      actualResponse shouldBe TestClass("t", 10)
    }

    "Invoke any hooks provided" in {
      val dummyResponseFuture = Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200))
      val testPOST = new StubbedHttpPost(dummyResponseFuture)

      testPOST.POSTBinary[TestClass](url, FileBytes("/testfile.txt")).futureValue

      verify(testPOST.testHook1)(url, "POST", None, dummyResponseFuture)
      verify(testPOST.testHook2)(url, "POST", None, dummyResponseFuture)
    }
  }
}
