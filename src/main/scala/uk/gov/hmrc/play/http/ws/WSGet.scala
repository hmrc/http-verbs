package uk.gov.hmrc.play.http.ws

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.http.{HttpGet, HttpResponse}
import MdcLoggingExecutionContext._

import scala.concurrent.Future

trait WSGet extends HttpGet with WSRequest {

  def doGet(url: String)(implicit  hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).get().map(new WSHttpResponse(_))
  }
}