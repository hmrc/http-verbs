package uk.gov.hmrc.play.http.ws

import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.http.{HeaderCarrier, Request}

trait WSRequestBuilder extends Request {

  def wsClient: WSClient

  def buildRequest[A](url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): WSRequest

}
