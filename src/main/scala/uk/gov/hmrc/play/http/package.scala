package uk.gov.hmrc.play

import uk.gov.hmrc.play.http.logging.ConnectionTracing

package object http {
  @deprecated("Re-named to ConnectionTracing", "23/04/2014")
  type ConnectionLogging = ConnectionTracing
}
