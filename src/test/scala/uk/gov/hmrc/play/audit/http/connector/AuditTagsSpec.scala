package uk.gov.hmrc.play.audit.http.connector

import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, Token, UserId}
import EventKeys._
import uk.gov.hmrc.play.http.HeaderNames
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.play.test.UnitSpec

class AuditTagsSpec extends UnitSpec {

  import uk.gov.hmrc.play.http.HeaderNames._

  val authorization = Authorization("authorization")
  val userId = UserId("userId")
  val token = Token("token")
  val forwarded = ForwardedFor("ipAdress")
  val sessionId = SessionId("1234567890")
  val requestId = RequestId("0987654321")

  "Audit TAGS" should {
    "be present" in {
      val hc = new HeaderCarrier(Some(authorization), Some(userId), Some(token), Some(forwarded), Some(sessionId), Some(requestId))

      val tags = hc.toAuditTags("theTransactionName", "/the/request/path")

      tags.size shouldBe 4

      tags(xSessionId) shouldBe sessionId.value
      tags(xRequestId) shouldBe requestId.value
      tags(TransactionName) shouldBe "theTransactionName"
      tags(Path) shouldBe "/the/request/path"
    }

    "be defaulted" in {
      val hc = HeaderCarrier()

      val tags = hc.toAuditTags("defaultsWhenNothingSet", "/the/request/path")

      tags.size shouldBe 4

      tags(xSessionId) shouldBe "-"
      tags(xRequestId) shouldBe "-"
      tags(TransactionName) shouldBe "defaultsWhenNothingSet"
      tags(Path) shouldBe "/the/request/path"
    }
  }

  "Audit DETAILS" should {
    "be present" in {
      val hc = new HeaderCarrier(Some(authorization), Some(userId), Some(token), Some(forwarded), Some(sessionId), Some(requestId))

      val details = hc.toAuditDetails()

      details.size shouldBe 3

      details("ipAddress") shouldBe forwarded.value
      details(authorisation) shouldBe authorization.value
      details(HeaderNames.token) shouldBe token.value
    }

    "be defaulted" in {
      val hc = HeaderCarrier()

      val details = hc.toAuditDetails()

      details.size shouldBe 3

      details("ipAddress") shouldBe "-"
      details(authorisation) shouldBe "-"
      details(HeaderNames.token) shouldBe "-"
    }

    "have more details only" in {
      val hc = HeaderCarrier()

      val details = hc.toAuditDetails("more-details" -> "the details", "lots-of-details" -> "interesting info")

      details.size shouldBe 5

      details("more-details") shouldBe "the details"
      details("lots-of-details") shouldBe "interesting info"

    }
  }

}
