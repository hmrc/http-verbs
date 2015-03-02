package uk.gov.hmrc.play.http.ws

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.play.http.HttpResponse

class WSHttpResponse(wsResponse: WSResponse) extends HttpResponse {
  override def allHeaders: Map[String, Seq[String]] = wsResponse.allHeaders

  override def status = wsResponse.status

  override def json = wsResponse.json

  override def body = wsResponse.body
}

trait WSHttp extends WSGet with WSPut with WSPost with WSDelete