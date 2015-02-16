package uk.gov.hmrc.play.connectors

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import play.api.Play.current

trait Connector {

  import play.api.libs.ws.{WS, WSRequestHolder}

  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequestHolder = WS.url(url).withHeaders(hc.headers: _*)
}
