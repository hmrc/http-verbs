package uk.gov.hmrc.play.http.ws


import uk.gov.hmrc.play.http.MockAuditing
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec

class WSDeleteSpec extends UnitSpec with WithFakeApplication {
    "doDelete" should {
      "audit the request even if a timeout exception is thrown" in {
        new WSDelete() with MockAuditing
      }
    }
}
