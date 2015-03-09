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

import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.test.Concurrent.await
import uk.gov.hmrc.play.test.Concurrent.liftFuture
import scala.concurrent.Future

class HttpPostSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  implicit val hc = HeaderCarrier()
  class StubbedHttpPost(doPostResult: Future[HttpResponse]) extends HttpPost with ConnectionTracingCapturing with MockAuditing {
    def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier) = doPostResult
    def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier) = doPostResult
    def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier) = doPostResult
    def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier) = doPostResult
  }

  val url = "http://some.url"
  val testBody = "test body"

  "HttpPost" should {

    "return the endpoint's response when the returned status code is in the 2xx range" in {
      (200 to 299).foreach {
        status =>
          val response = new DummyHttpResponse("", status)

          val result = new StubbedHttpPost(response).POST(url, testBody).futureValue
          await(result) shouldBe response
      }
    }

    "throw an NotFoundException when the response has 404 status" in {
      val response = new DummyHttpResponse(testBody, 404)

      val e = new StubbedHttpPost(response).POST(url, testBody).failed.futureValue

      e.getMessage should startWith(POST)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val response = new DummyHttpResponse(testBody, 400)

      val e = new StubbedHttpPost(response).POST(url, testBody).failed.futureValue

      e.getMessage should startWith(POST)
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POST(url, "anyString"))
    behave like aTracingHttpCall[StubbedHttpPost](POST, "POST", new StubbedHttpPost(defaultHttpResponse)) { _.POST(url, "anyString") }

    "throw a Exception when the response has an arbitrary status" in {
      val response = new DummyHttpResponse(testBody, 500)

      val e = new StubbedHttpPost(response).POST(url, testBody).failed.futureValue

      e.getMessage should startWith(POST)
      e.getMessage should include(url)
      e.getMessage should include("500")
      e.getMessage should include(testBody)
    }
  }

  "POSTForm" should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTForm(url, Map()))
    behave like aTracingHttpCall[StubbedHttpPost](POST, "POSTForm", new StubbedHttpPost(defaultHttpResponse)) { _.POSTForm(url, Map()) }
  }

  "POSTString"  should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTString(url, "body", Seq.empty))
    behave like aTracingHttpCall[StubbedHttpPost](POST, "POSTString", new StubbedHttpPost(defaultHttpResponse)) { _.POSTString(url, "body", Seq.empty) }
  }

  "POSTEmpty"  should {
    behave like anErrorMappingHttpCall(POST, (url, responseF) => new StubbedHttpPost(responseF).POSTEmpty(url))
    behave like aTracingHttpCall[StubbedHttpPost](POST, "POSTEmpty", new StubbedHttpPost(defaultHttpResponse)) { _.POSTEmpty(url) }
  }

}
