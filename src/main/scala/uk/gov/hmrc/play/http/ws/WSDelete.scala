package uk.gov.hmrc.play.http.ws

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.http.{HttpDelete, HttpResponse}
import MdcLoggingExecutionContext._

import scala.concurrent.Future

trait WSDelete extends HttpDelete with WSRequest {

  def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).delete().map(new WSHttpResponse(_))
  }
}