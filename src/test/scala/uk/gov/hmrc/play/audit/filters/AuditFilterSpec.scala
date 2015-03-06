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
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{EssentialAction, RequestHeader, Result, Results}
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult, MockAuditConnector}
import uk.gov.hmrc.play.audit.model.{AuditEvent, DataEvent}
import uk.gov.hmrc.play.test.Concurrent._
import uk.gov.hmrc.play.test.Concurrent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditFilterSpec extends WordSpecLike with Matchers with Eventually with ScalaFutures {

  "AuditFilter" should {
    val applicationName = "app-name"

    "audit a request with both session and header information" in running(FakeApplication()) {
      val xRequestId = "A_REQUEST_ID"
      val xSessionId = "A_SESSION_ID"

      implicit val hc = HeaderCarrier
      val request = FakeRequest().withHeaders("X-Request-ID" -> xRequestId).withSession("sessionId" -> xSessionId)
      val mockAuditConnector = new MockAuditConnector {
        var events: List[AuditEvent] = List.empty[AuditEvent]
        override def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec : ExecutionContext) = {
          events = events :+ event
          Future.successful(AuditResult.Success)
        }
      }
      val action: (RequestHeader) => Iteratee[Array[Byte], Result] = { requestHeader =>
        Iteratee.fold[Array[Byte], Result](new Results.Status(404)) {
          (length, bytes) => new Results.Status(200)
        }
      }

      val nextAction = EssentialAction(action)
      val auditFilter = new AuditFilter {
        override val auditConnector: AuditConnector = mockAuditConnector
        override val appName: String = applicationName
        override def controllerNeedsAuditing(controllerName: String): Boolean = true
      }

      val iteratee = auditFilter.withAuditedRequest(nextAction, request)
      Concurrent.await(iteratee.run)
      val event = DataEvent(applicationName,"ServiceReceivedRequest")
      eventually {
        val events = mockAuditConnector.events
        events should have size 1
        events(0).auditSource shouldBe applicationName
        events(0).auditType shouldBe "ServiceReceivedRequest"
        events(0).tags("X-Request-ID") shouldBe xRequestId
        events(0).tags("X-Session-ID") shouldBe xSessionId
      }
    }

    "audit a response with both session and header information" in running(FakeApplication()) {
      val xRequestId = "A_REQUEST_ID"
      val xSessionId = "A_SESSION_ID"

      implicit val hc = HeaderCarrier
      val request = FakeRequest().withHeaders("X-Request-ID" -> xRequestId).withSession("sessionId" -> xSessionId)

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

      val iteratee = Iteratee.fold[Array[Byte], Result](new Results.Status(404)) {
        (length, bytes) => new Results.Status(200)
      }

      val responseIteratee = auditFilter.withAuditedResponse(iteratee, request)
      val bodyEnumerator = responseIteratee.map(_.body)
      Concurrent.await(bodyEnumerator.run(iteratee))

      eventually {
        val events = mockAuditConnector.events
        events should have size 1
        events(0).auditSource shouldBe applicationName
        events(0).auditType shouldBe "ServiceSentResponse"
        events(0).tags("X-Request-ID") shouldBe xRequestId
        events(0).tags("X-Session-ID") shouldBe xSessionId
      }
    }
  }
}
