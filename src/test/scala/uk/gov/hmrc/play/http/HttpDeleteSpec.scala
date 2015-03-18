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
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.Future

class HttpDeleteSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  class StubbedHttpDelete(response: Future[HttpResponse]) extends HttpDelete with ConnectionTracingCapturing {
    def auditConnector: AuditConnector = ???
    def appName: String = ???

    def doDelete(url: String)(implicit hc: HeaderCarrier) = response
  }

  "HttpDelete" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testDelete = new StubbedHttpDelete(Future.successful(response))
      testDelete.DELETE(url).futureValue shouldBe response
    }
    "be able to return HTML responses" in new HtmlHttpReads {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse(testBody, 200)))
      testDelete.DELETE(url).futureValue should be (an [Html])
    }
    "be able to return objects deserialised from JSON" in {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      testDelete.DELETE[TestClass](url).futureValue should be (TestClass("t", 10))
    }
    behave like anErrorMappingHttpCall(DELETE, (url, responseF) => new StubbedHttpDelete(responseF).DELETE(url))
    behave like aTracingHttpCall(DELETE, "DELETE", new StubbedHttpDelete(defaultHttpResponse)) { _.DELETE(url) }
  }
}
