package uk.gov.hmrc.play.http.ws

import play.api.libs.ws
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.{HeaderCarrier, Request}

trait WSRequestBuilder extends Request {

  def wsClient: WSClient

  def buildRequest[A](url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): ws.WSRequest

}
