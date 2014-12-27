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

  override def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier, ec : ExecutionContext): Unit = {
    recordedEvent = Some(event)
  }

  override def sendMergedEvent(event: MergedDataEvent)(implicit hc: HeaderCarrier, ec : ExecutionContext): Unit = {
    recordedMergedEvent = Some(event)
  }

  override protected def logError(s: String, t: Throwable): Unit = ???

  override protected def logError(s: String): Unit = ???

  override protected def callAuditConsumer(url:String, body: JsValue)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[HttpResponse] = ???

  override def auditingConfig: AuditingConfig = ???
}

