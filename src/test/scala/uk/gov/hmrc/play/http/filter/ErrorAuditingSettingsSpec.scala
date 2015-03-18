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

package uk.gov.hmrc.play.http.filter

import org.scalatest.{Matchers, WordSpecLike}
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.{GlobalSettings, PlayException}
import uk.gov.hmrc.play.audit.EventTypes
import uk.gov.hmrc.play.audit.http.config.ErrorAuditingSettings
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, MockAuditConnector}
import uk.gov.hmrc.play.http.connector.DummyRequestHeader
import uk.gov.hmrc.play.http.{JsValidationException, NotFoundException}
import uk.gov.hmrc.play.test.Concurrent.await

import scala.concurrent.Future


class ErrorAuditingSettingsSpec extends WordSpecLike with Matchers {

  trait ParentHandler extends GlobalSettings with Results {
    var onBadRequestCalled = false

    override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
      onBadRequestCalled = true
      Future.successful(BadRequest)
    }

    var onHandlerNotFoundCalled = false

    override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
      onHandlerNotFoundCalled = true
      Future.successful(NotFound)
    }

    var onErrorCalled = false

    override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
      onErrorCalled = true
      Future.successful(InternalServerError)
    }
  }

  class TestErrorAuditing(override val auditConnector: AuditConnector) extends ParentHandler with ErrorAuditingSettings {
    override lazy val appName = "app"
  }

  "in a case of application error we" should {

    "send ServerInternalError event to DataStream for an Exception that occurred in the microservice" in {
      val mockConnector = new MockAuditConnector
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onError(new DummyRequestHeader(), new PlayException("", "", new Exception("a generic application exception")))
      await(resultF)
      mockConnector.recordedEvent shouldNot be(None)
      mockConnector.recordedEvent.map(_.auditType shouldBe EventTypes.ServerInternalError)
    }

    "send ResourceNotFound event to DataStream for a NotFoundException that occurred in the microservice" in {
      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onError(new DummyRequestHeader(), new PlayException("", "", new NotFoundException("test")))
      await(resultF)
      mockConnector.recordedEvent shouldNot be(None)
      mockConnector.recordedEvent.map(_.auditType shouldBe EventTypes.ResourceNotFound)
    }

    "send ServerValidationError event to DataStream for a JsValidationException that occurred in the microservice" in {
      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onError(new DummyRequestHeader(), new PlayException("", "", new JsValidationException("GET", "", classOf[String], Seq.empty)))
      await(resultF)
      mockConnector.recordedEvent shouldNot be(None)
      mockConnector.recordedEvent.map(_.auditType shouldBe EventTypes.ServerValidationError)
    }

    "chain onError call to parent" in {
      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onError(new DummyRequestHeader(), new PlayException("", "", new NotFoundException("test")))
      await(resultF)
      auditing.onErrorCalled shouldBe true
    }

  }

  "in a case of the microservice endpoint not being found we" should {

    "send ResourceNotFound event to DataStream" in {

      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onHandlerNotFound(new DummyRequestHeader())
      await(resultF)
      mockConnector.recordedEvent shouldNot be(None)
      mockConnector.recordedEvent.map(_.auditType shouldBe EventTypes.ResourceNotFound)
    }

    "chain onHandlerNotFound call to parent" in {
      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onHandlerNotFound(new DummyRequestHeader())
      await(resultF)
      auditing.onHandlerNotFoundCalled shouldBe true
    }

  }

  "in a case of incorrect data being sent to the microservice endpoint we" should {

    "send ServerValidationError event to DataStream" in {
      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onBadRequest(new DummyRequestHeader(), "error message")
      await(resultF)
      mockConnector.recordedEvent shouldNot be(None)
      mockConnector.recordedEvent.map(_.auditType shouldBe EventTypes.ServerValidationError)
    }

    "chain onBadRequest call to parent" in {
      val mockConnector = new MockAuditConnector()
      val auditing = new TestErrorAuditing(mockConnector)

      val resultF = auditing.onBadRequest(new DummyRequestHeader(), "error message")
      await(resultF)
      auditing.onBadRequestCalled shouldBe true
    }
  }

}
