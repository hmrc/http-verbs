package uk.gov.hmrc.play.http.ws

import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, Token}
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.play.test.UnitSpec

class WSRequestSpec extends UnitSpec {

  "buildRequest" should {

    "create a WSRequestBuilder with the right URL and Http headers" in running(FakeApplication()) {
      val url = "http://test.me"

      implicit val hc = HeaderCarrier(authorization = Some(Authorization("auth")),
        sessionId = Some(SessionId("session")),
        requestId = Some(RequestId("request")),
        token = Some(Token("token")),
        forwarded = Some(ForwardedFor("forwarded")))

      val wsRequest = new WSRequest {}
      val result = wsRequest.buildRequest(url)

      val expectedHeaders = hc.headers.map {
        case (a, b) => (a, List(b))
      }.toMap

      result.headers shouldBe expectedHeaders

      result.url shouldBe url
    }

  }

}
