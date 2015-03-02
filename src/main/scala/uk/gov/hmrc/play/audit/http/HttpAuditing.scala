package uk.gov.hmrc.play.audit.http

import org.joda.time.DateTime
import uk.gov.hmrc.play.audit.EventKeys._
import uk.gov.hmrc.play.audit.EventTypes._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataCall, MergedDataEvent}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait HttpAuditing extends DateTimeUtils {

  def auditConnector: AuditConnector
  def appName: String
  def auditDisabledForPattern = ("""http://.*\.service""").r

  protected def auditRequestWithResponseF(url: String, verb: String, body: Option[_], responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {
    val request = HttpRequest(url, verb, body, now)
    responseToAuditF.map {
      response =>
        audit(request, response)
    }.recover {
      case e: Throwable => auditRequestWithException(request, e.getMessage)
    }
  }

  def auditFromPlayFrontend(url: String, response: HttpResponse, hc: HeaderCarrier) = audit(HttpRequest(url, "", None, now), response)(hc)

  private[http] def audit(request: HttpRequest, responseToAudit: HttpResponse)(implicit hc: HeaderCarrier): Unit =
    if (isAuditable(request.url)) auditConnector.sendMergedEvent(dataEventFor(request, responseToAudit))

  private[http] def auditRequestWithException(request: HttpRequest, errorMessage: String)(implicit hc: HeaderCarrier): Unit =
    if (isAuditable(request.url)) auditConnector.sendMergedEvent(dataEventFor(request, errorMessage))


  private def dataEventFor(request: HttpRequest, errorMesssage: String)(implicit hc: HeaderCarrier) = {
    val responseDetails = Map(FailedRequestMessage -> errorMesssage)
    buildDataEvent(request, responseDetails)
  }

  private def dataEventFor(request: HttpRequest, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val responseDetails = Map(ResponseMessage -> response.body, StatusCode -> response.status.toString)
    buildDataEvent(request, responseDetails)
  }


  private def buildDataEvent(request: HttpRequest, responseDetails: Map[String, String])(implicit hc: HeaderCarrier) =
    MergedDataEvent(
      auditSource = appName,
      auditType = OutboundCall,
      request = DataCall(hc.toAuditTags(request.url, request.url), hc.toAuditDetails(requestDetails(request): _*), request.generatedAt),
      response = DataCall(Map.empty, responseDetails, now))

  private def requestDetails(request: HttpRequest)(implicit hc: HeaderCarrier): Seq[(String, String)] =
    Seq(Path -> request.url, Method -> request.verb) ++ request.body.map(b => Seq(RequestBody -> b.toString)).getOrElse(Seq.empty) ++ HeaderFieldsExtractor.optionalAuditFields(hc.extraHeaders.toMap)

  private def isAuditable(url: String) = !url.contains("/write/audit") && !auditDisabledForPattern.findFirstIn(url).isDefined

  protected case class HttpRequest(url: String, verb: String, body: Option[_], generatedAt: DateTime)

}
