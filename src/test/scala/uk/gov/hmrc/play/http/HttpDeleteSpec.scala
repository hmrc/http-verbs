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
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.Future

class HttpDeleteSpec extends WordSpecLike with Matchers with CommonHttpBehaviour {

  implicit val hc = HeaderCarrier()

  class StubbedHttpDelete(response: Future[HttpResponse]) extends HttpDelete with ConnectionTracingCapturing {
    def auditConnector: AuditConnector = ???
    def appName: String = ???

    def doDelete(url: String)(implicit hc: HeaderCarrier) = response
  }

  "HttpDelete" should {
    val testBody = "testBody"
    val url = "http://some.url"

    "return the endpoint's response when the returned status code is in the 2xx range" in {
      (200 to 299).foreach { status =>
        val response = new DummyHttpResponse(testBody, status)
        val testDelete = new StubbedHttpDelete(Future.successful(response))
        testDelete.DELETE(url).futureValue shouldBe response
      }
    }

    "throw an NotFoundException when the response has 404 status" in {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse(testBody, 404)))

      val e = testDelete.DELETE(url).failed.futureValue
      e.getMessage should startWith(DELETE)
      e.getMessage should include(url)
      e.getMessage should include("404")
      e.getMessage should include(testBody)
    }

    "throw an BadRequestException when the response has 400 status" in {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse(testBody, 400)))

      val e = testDelete.DELETE(url).failed.futureValue
      e.getMessage should startWith(DELETE)
      e.getMessage should include(url)
      e.getMessage should include("400")
      e.getMessage should include(testBody)
    }

    behave like anErrorMappingHttpCall(DELETE, (url, responseF) => new StubbedHttpDelete(responseF).DELETE(url))
    behave like aTracingHttpCall(DELETE, "DELETE", new StubbedHttpDelete(defaultHttpResponse)) { _.DELETE(url) }

    "throw a Exception when the response has an arbitrary status" in {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse(testBody, 500)))

      val e = testDelete.DELETE(url).failed.futureValue
      e.getMessage should startWith(DELETE)
      e.getMessage should include(url)
      e.getMessage should include("500")
      e.getMessage should include(testBody)
    }
  }
}
