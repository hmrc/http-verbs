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

package uk.gov.hmrc.play.audit.http.connector

import play.api.libs.json.JsValue
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.model.{AuditEvent, MergedDataEvent}
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.{ExecutionContext, Future}

class MockAuditConnector extends AuditConnector {
  var recordedEvent: Option[AuditEvent] = None
  var recordedMergedEvent: Option[MergedDataEvent] = None

  override def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier, ec : ExecutionContext) = {
    recordedEvent = Some(event)
    Future.successful(AuditResult.Success)
  }

  override def sendMergedEvent(event: MergedDataEvent)(implicit hc: HeaderCarrier, ec : ExecutionContext) = {
    recordedMergedEvent = Some(event)
    Future.successful(AuditResult.Success)
  }

  override protected def logError(s: String, t: Throwable): Unit = ???

  override protected def logError(s: String): Unit = ???

  override protected def callAuditConsumer(url:String, body: JsValue)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[HttpResponse] = ???

  override def auditingConfig: AuditingConfig = ???
}
