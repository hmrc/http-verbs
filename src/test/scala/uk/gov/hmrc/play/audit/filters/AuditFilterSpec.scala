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

package uk.gov.hmrc.play.audit.filters

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult, MockAuditConnector}
import uk.gov.hmrc.play.audit.model.{AuditEvent, DataEvent}
import uk.gov.hmrc.play.test.Concurrent
import uk.gov.hmrc.play.test.Concurrent._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditFilterSpec extends WordSpecLike with Matchers with Eventually with ScalaFutures with FilterFlowMock {

  "AuditFilter" should {
    val applicationName = "app-name"

    "audit a request and response with header information" in running(FakeApplication()) {
      val xRequestId = "A_REQUEST_ID"
      val xSessionId = "A_SESSION_ID"

      implicit val hc = HeaderCarrier
      val request = FakeRequest().withHeaders("X-Request-ID" -> xRequestId, "X-Session-ID" -> xSessionId)
      val mockAuditConnector = new MockAuditConnector {
        var events: List[AuditEvent] = List.empty[AuditEvent]
        override def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec : ExecutionContext) = {
          events = events :+ event
          Future.successful(AuditResult.Success)
        }
      }

      val auditFilter = new AuditFilter {
        override val auditConnector: AuditConnector = mockAuditConnector
        override val appName: String = applicationName
        override def controllerNeedsAuditing(controllerName: String): Boolean = true
      }

      val iteratee = auditFilter.audit(request, nextAction)
      val bodyEnumerator = iteratee.map(_.body)
      Concurrent.await(bodyEnumerator.run(iteratee))

      eventually {
        val events = mockAuditConnector.events
        events should have size 2

        events(0).auditSource shouldBe applicationName
        events(0).auditType shouldBe "ServiceReceivedRequest"
        events(0).tags("X-Request-ID") shouldBe xRequestId
        events(0).tags("X-Session-ID") shouldBe xSessionId

        events(1).auditSource shouldBe applicationName
        events(1).auditType shouldBe "ServiceSentResponse"
        events(1).tags("X-Request-ID") shouldBe xRequestId
        events(1).tags("X-Session-ID") shouldBe xSessionId
      }
    }
  }
}
