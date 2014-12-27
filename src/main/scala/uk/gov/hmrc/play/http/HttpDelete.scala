package uk.gov.hmrc.play.http

import play.api.http.HttpVerbs.{DELETE => DELETE_VERB}
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditing}
import uk.gov.hmrc.play.http.logging.{MdcLoggingExecutionContext, ConnectionTracing}
import scala.concurrent.Future
import MdcLoggingExecutionContext._

trait HttpDelete extends HttpVerb with ConnectionTracing with HttpAuditing {

  protected def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  def DELETE(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    withTracing(DELETE_VERB, url) {
      val httpResponse = doDelete(url)
      auditRequestWithResponseF(url, DELETE_VERB, None, httpResponse)
      mapErrors(DELETE_VERB, url, httpResponse).map(handleResponse(url, DELETE_VERB))
    }
  }
}

