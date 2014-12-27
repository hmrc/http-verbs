package uk.gov.hmrc.play.audit.http.config

import play.api.GlobalSettings
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.play.audit.EventTypes
import uk.gov.hmrc.play.audit.http.HttpAuditEvent
import EventTypes._
import uk.gov.hmrc.play.http.{JsValidationException, NotFoundException}

import scala.concurrent.Future

trait ErrorAuditingSettings extends GlobalSettings with HttpAuditEvent {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val unexpectedError = "Unexpected error"
  private val notFoundError = "Resource Endpoint Not Found"
  private val badRequestError = "Request bad format exception"

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    val code = ex.getCause match {
      case e: NotFoundException => ResourceNotFound
      case jsError: JsValidationException => ServerValidationError
      case _ => ServerInternalError
    }

    auditConnector.sendEvent(dataEvent(code, unexpectedError, request)
      .withDetail((TransactionFailureReason, ex.getMessage)))
    super.onError(request, ex)
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    auditConnector.sendEvent(dataEvent(ResourceNotFound, notFoundError, request))
    super.onHandlerNotFound(request)
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    auditConnector.sendEvent(dataEvent(ServerValidationError, badRequestError, request))
    super.onBadRequest(request, error)
  }
}
