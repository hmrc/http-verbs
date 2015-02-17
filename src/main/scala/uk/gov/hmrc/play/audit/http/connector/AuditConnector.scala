package uk.gov.hmrc.play.audit.http.connector

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{MergedDataEvent, AuditEvent}
import uk.gov.hmrc.play.connectors.Connector
import uk.gov.hmrc.play.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

import scala.util.{Failure, Success}

trait AuditEventFailureKeys {
  private val EventMissed = "DS_EventMissed"
  val LoggingAuditRequestFailureKey : String = EventMissed + "_AuditFailureResponse"
  val LoggingAuditFailureResponseKey : String = EventMissed + "_AuditRequestFailure"
}

object AuditEventFailureKeys extends AuditEventFailureKeys

sealed trait AuditResult
object AuditResult {
  case object Success extends AuditResult
  case object Disabled extends AuditResult
  case class Failure(msg: String, nested: Option[Throwable] = None) extends Exception(msg, nested.orNull) with AuditResult
}

trait AuditConnector extends Connector with AuditEventFailureKeys{

  import uk.gov.hmrc.play.audit.http.config.AuditingConfig
  import uk.gov.hmrc.play.http.HttpResponse
  import uk.gov.hmrc.play.http.logging.LoggingDetails
  import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

  def auditingConfig: AuditingConfig

  protected def callAuditConsumer(url: String, body: JsValue)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[HttpResponse]

  protected def logError(s: String)

  protected def logError(s: String, t: Throwable)

  def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec : ExecutionContext): Future[AuditResult] =
    sendEvent(auditingConfig.consumer.singleEventUrl, Json.toJson(event))

  def sendMergedEvent(event: MergedDataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec : ExecutionContext): Future[AuditResult] =
    sendEvent(auditingConfig.consumer.mergedEventUrl, Json.toJson(event))

  def sendLargeMergedEvent(event: MergedDataEvent)(implicit hc: HeaderCarrier = HeaderCarrier()): Future[AuditResult] =
    sendEvent(auditingConfig.consumer.largeMergedEventUrl, Json.toJson(event))

  private def sendEvent(url: String, body: JsValue)(implicit hc: HeaderCarrier) = {
    if (auditingConfig.enabled) {
      handleResult(callAuditConsumer(url, body), body).map { _ => AuditResult.Success }
    } else {
      Logger.info(s"auditing disabled for request-id ${hc.requestId}, session-id: ${hc.sessionId}")
      Future.successful(AuditResult.Disabled)
    }
  }

  protected[connector] def handleResult(resultF: Future[HttpResponse], body: JsValue)(implicit ld: LoggingDetails): Future[HttpResponse] = {
    resultF
      .recoverWith { case t =>
        val message = makeFailureMessage(body)
        logError(message, t)
        Future.failed(AuditResult.Failure(message, Some(t)))
      }
      .map { response =>
        checkResponse(body, response) match {
          case Some(error) =>
            logError(error)
            throw AuditResult.Failure(error)
          case None => response
        }
      }
  }

  private[connector] def makeFailureMessage(body: JsValue): String = s"$LoggingAuditRequestFailureKey : audit item : $body"

  private[connector] def checkResponse(body: JsValue, response: HttpResponse): Option[String] = {
    if (response.status >= 300) Some(s"$LoggingAuditFailureResponseKey : status code : ${response.status} : audit item : $body")
    else None
  }
}

object AuditConnector extends AuditConnector with ConnectionTracing {

  import play.api.Logger
  import uk.gov.hmrc.play.config.ServicesConfig
  import uk.gov.hmrc.play.http.HttpResponse
  import uk.gov.hmrc.play.http.ws.WSHttpResponse

  lazy val auditingConfig = ServicesConfig.auditingConfig

  protected def callAuditConsumer(url:String , body: JsValue)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[HttpResponse] =
    withTracing("Post", url) {
      buildRequest(url).post(body).map(new WSHttpResponse(_))
    }

  protected def logError(s: String) = Logger.warn(s)

  protected def logError(s: String, t: Throwable) = Logger.warn(s, t)
}
