/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.ghik.silencer.silent
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.http._

@silent("deprecated")
class ConnectorSpec extends AnyWordSpecLike with Matchers {
  class TestConfig(val builderName: String, val builder: RequestBuilder, setupFunc: ((=> Any) => Any)) {
    def setup(f:                                                                      => Any) = setupFunc(f)
  }

  val withFakeApp: (=>     Any) => Any = running(FakeApplication())
  def withoutFakeApp(f: => Any)        = f

  val permutations = Seq(
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
        val sessionId         = SessionId("session")
        val requestId         = RequestId("requestId")
        val deviceID          = "deviceIdTest"

        val hc = HeaderCarrier(
          authorization = Some(testAuthorisation),
          forwarded     = Some(forwarded),
          sessionId     = Some(sessionId),
          requestId     = Some(requestId),
          deviceID      = Some(deviceID),
          otherHeaders  = Seq("path" -> "/the/request/path")
        )

        val request = p.builder.buildRequest("http://test.public.service/bar")(hc)
        request.headers.get(HeaderNames.authorisation) shouldBe Some(Seq(testAuthorisation.value))
        request.headers.get(HeaderNames.xForwardedFor) shouldBe Some(Seq(forwarded.value))
        request.headers.get(HeaderNames.xSessionId)    shouldBe Some(Seq(sessionId.value))
        request.headers.get(HeaderNames.xRequestId)    shouldBe Some(Seq(requestId.value))
        request.headers.get(HeaderNames.deviceID)      shouldBe Some(Seq(deviceID))
        request.headers.get("path")                    shouldBe None
      }
    }
  }
}
