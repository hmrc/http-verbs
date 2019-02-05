/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.connectors

import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}

class ConnectorSpec extends WordSpecLike with Matchers {
  class TestConfig(val builderName: String, val builder: RequestBuilder, setupFunc: ((=> Any) => Any)) {
    def setup(f:                                                                      => Any) = setupFunc(f)
  }

  val withFakeApp: (=>     Any) => Any = running(FakeApplication())
  def withoutFakeApp(f: => Any)        = f

  val permutations = Seq(
    new TestConfig("Deprecated Connector", new Connector {}, withFakeApp),
    new TestConfig("PlayWS Request Builder", new PlayWSRequestBuilder {}, withFakeApp),
    new TestConfig(
      "WSClient Request Builder",
      new WSClientRequestBuilder with DefaultWSClientProvider {},
      withoutFakeApp)
  )

  "AuthConnector.buildRequest" should {
    permutations.foreach { p =>
      s"add expected headers to the request using the ${p.builderName}" in p.setup {
        val testAuthorisation = Authorization("someauth")
        val forwarded         = ForwardedFor("forwarded")
        val token             = Token("token")
        val sessionId         = SessionId("session")
        val requestId         = RequestId("requestId")
        val deviceID          = "deviceIdTest"

        val carrier: HeaderCarrier = HeaderCarrier(
          authorization = Some(testAuthorisation),
          token         = Some(token),
          forwarded     = Some(forwarded),
          sessionId     = Some(sessionId),
          requestId     = Some(requestId),
          deviceID      = Some(deviceID),
          otherHeaders  = Seq("path" -> "/the/request/path")
        )

        val request = p.builder.buildRequest("authBase")(carrier)
        request.headers.get(HeaderNames.authorisation).flatMap(_.headOption) shouldBe Some(testAuthorisation.value)
        request.headers.get(HeaderNames.xForwardedFor).flatMap(_.headOption) shouldBe Some(forwarded.value)
        request.headers.get(HeaderNames.token).flatMap(_.headOption)         shouldBe Some(token.value)
        request.headers.get(HeaderNames.xSessionId).flatMap(_.headOption)    shouldBe Some(sessionId.value)
        request.headers.get(HeaderNames.xRequestId).flatMap(_.headOption)    shouldBe Some(requestId.value)
        request.headers.get(HeaderNames.deviceID).flatMap(_.headOption)      shouldBe Some(deviceID)
        request.headers.get("path")                                          shouldBe None
      }
    }
  }
}
