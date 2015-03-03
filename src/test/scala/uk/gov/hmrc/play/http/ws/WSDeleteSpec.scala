package uk.gov.hmrc.play.http.ws

import org.scalatest.WordSpecLike
import uk.gov.hmrc.play.http.MockAuditing

class WSDeleteSpec extends WordSpecLike {
    "doDelete" should {
      "audit the request even if a timeout exception is thrown" in {
        new WSDelete() with MockAuditing
      }
    }
}
