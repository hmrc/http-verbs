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


trait AuditConnector extends Connector with AuditEventFailureKeys{

  import uk.gov.hmrc.play.audit.http.config.AuditingConfig
  import uk.gov.hmrc.play.http.HttpResponse
  import uk.gov.hmrc.play.http.logging.LoggingDetails
  import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

  def auditingConfig: AuditingConfig

  protected def callAuditConsumer(url: String, body: JsValue)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[HttpResponse]

  protected def logError(s: String)

  protected def logError(s: String, t: Throwable)

  def sendEvent(event: AuditEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec : ExecutionContext): Unit =
    sendEvent(auditingConfig.consumer.singleEventUrl, Json.toJson(event))

  def sendMergedEvent(event: MergedDataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec : ExecutionContext): Unit =
    sendEvent(auditingConfig.consumer.mergedEventUrl, Json.toJson(event))

  def sendLargeMergedEvent(event: MergedDataEvent)(implicit hc: HeaderCarrier = HeaderCarrier()) {
    sendEvent(auditingConfig.consumer.largeMergedEventUrl, Json.toJson(event))
  }

  private def sendEvent(url: String, body: JsValue)(implicit hc: HeaderCarrier) = {
    if (auditingConfig.enabled) {
      handleResult(callAuditConsumer(url, body), body)
    } else {
      Logger.info(s"auditing disabled for request-id ${hc.requestId}, session-id: ${hc.sessionId}")
    }
  }

  protected[connector] def handleResult(resultF: Future[HttpResponse], body: JsValue)(implicit ld: LoggingDetails): Future[HttpResponse] = {
    resultF.andThen {
      case Success(response) => checkResponse(body, response).map(logError)
      case Failure(t) => logError(makeFailureMessage(body), t)
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
