package uk.gov.hmrc.play.http.ws

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.http.{HttpPost, HttpResponse}
import MdcLoggingExecutionContext._

import scala.concurrent.Future
import play.api.mvc.Results


trait WSPost extends HttpPost with WSRequest {

  override def doPost[A](url: String, body: A, headers: Seq[(String,String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).withHeaders(headers: _*).post(Json.toJson(body)).map(new WSHttpResponse(_))
  }

  override def doFormPost(url: String, body: Map[String,Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).post(body).map(new WSHttpResponse(_))
  }

  override def doPostString(url: String, body: String, headers: Seq[(String,String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).withHeaders(headers: _*).post(body).map(new WSHttpResponse(_))
  }

  override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    import play.api.http.Writeable._
    buildRequest(url).post(Results.EmptyContent()).map(new WSHttpResponse(_))
  }

}
