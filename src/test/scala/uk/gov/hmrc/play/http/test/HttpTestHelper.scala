package uk.gov.hmrc.play.http.test

import org.scalatest.{Matchers, Suite}
import play.api.libs.json.{JsNull, JsValue}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSRequest

trait HttpTestHelper extends ResponseMatchers {
  self: Suite with Matchers with WSRequest =>

  def verifyGETStatusCodeOnly(url: String, expectedStatus: Int, headers: Option[HeaderCarrier] = None)(implicit hc : HeaderCarrier = headers.getOrElse(HeaderCarrier())) : Unit = {
    buildRequest(url).get() should have (status (expectedStatus))
  }

  def verifyPOSTStatusCodeOnly(url: String, expectedStatus: Int, body: JsValue = JsNull, headers: Option[HeaderCarrier] = None)(implicit hc : HeaderCarrier = headers.getOrElse(HeaderCarrier()))  : Unit = {
    buildRequest(url).post(body) should have (status (expectedStatus))
  }
}
