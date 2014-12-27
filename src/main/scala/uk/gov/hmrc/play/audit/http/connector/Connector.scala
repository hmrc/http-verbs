package uk.gov.hmrc.play.audit.http.connector

import play.api.Play.current
import uk.gov.hmrc.play.audit.http.HeaderCarrier

trait Connector {

  import play.api.libs.ws.{WS, WSRequestHolder}

  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequestHolder = WS.url(url).withHeaders(hc.headers: _*)
}