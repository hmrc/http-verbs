package uk.gov.hmrc.play.http

import play.api.libs.json.{Json, Writes}
import play.api.http.HttpVerbs.{PUT => PUT_VERB}
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditing}
import uk.gov.hmrc.play.http.logging.{MdcLoggingExecutionContext, ConnectionTracing}
import MdcLoggingExecutionContext._

import scala.concurrent.Future

trait HttpPut extends HttpVerb with ConnectionTracing with HttpAuditing {

  protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse]

  private def defaultHandler(responseF: Future[HttpResponse], url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = responseF.map(handleResponse(PUT_VERB, url))

  def PUT[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    PUT(url, body, defaultHandler)
  }

  def PUT[A](url: String, body: A, auditRequestBody: Boolean, auditResponseBody: Boolean)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    PUT(url, body, defaultHandler, auditRequestBody, auditResponseBody)
  }

  def PUT[A](url: String, body: A, responseHandler: ProcessingFunction,  auditRequestBody: Boolean = true, auditResponseBody: Boolean = true)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    withTracing(PUT_VERB, url) {
      val httpResponse = doPut(url, body)
      auditRequestWithResponseF(url, PUT_VERB, Option(Json.stringify(rds.writes(body))), httpResponse)
      responseHandler(mapErrors(PUT_VERB, url, httpResponse), url)
    }
  }
}
