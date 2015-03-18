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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future

class HttpGetSpec extends WordSpecLike with Matchers with ScalaFutures with CommonHttpBehaviour with IntegrationPatience {

  class StubbedHttpGet(doGetResult: Future[HttpResponse] = defaultHttpResponse) extends HttpGet with MockAuditing with ConnectionTracingCapturing {
    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = doGetResult
    override protected def auditRequestWithResponseF(url: String, verb:String, body:Option[_] ,responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier)= {}
  }

  "HttpGet" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testGet = new StubbedHttpGet(Future.successful(response))
      testGet.GET(url).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testGet = new StubbedHttpGet(Future.successful(new DummyHttpResponse(testBody, 200)))
      testGet.GET(url).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testGet = new StubbedHttpGet(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testGet.GET[TestClass](url).futureValue should be (TestClass("t", 10))
    }
    behave like anErrorMappingHttpCall(GET, (url, responseF) => new StubbedHttpGet(responseF).GET(url))
    behave like aTracingHttpCall(GET, "GET", new StubbedHttpGet(defaultHttpResponse)) { _.GET(url) }
  }
}
