package uk.gov.hmrc.play.http.ws

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.http.{HttpPut, HttpResponse}
import MdcLoggingExecutionContext._

import scala.concurrent.Future

trait WSPut extends HttpPut with WSRequest {

  def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).put(Json.toJson(body)).map (new WSHttpResponse(_))
  }
}
