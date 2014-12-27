package uk.gov.hmrc.play.http.test

import uk.gov.hmrc.play.audit.http.HeaderCarrier

trait WithHeaderCarrier {
  implicit val hc = HeaderCarrier()
}
