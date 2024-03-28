/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.test.WsTestClient
import uk.gov.hmrc.http._

@annotation.nowarn("msg=deprecated")
class ConnectorSpec extends AnyWordSpecLike with Matchers with MockitoSugar {
  WsTestClient.withClient { wsClient =>

    "AuthConnector.buildRequest" should {
      val builder = new WSClientRequestBuilder {
        val client = wsClient
      }

      s"add expected headers to the request" in {
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

        val request = builder.buildRequest("http://test.public.service/bar")(hc)
        request.header(HeaderNames.authorisation) shouldBe Some(testAuthorisation.value)
        request.header(HeaderNames.xForwardedFor) shouldBe Some(forwarded.value)
        request.header(HeaderNames.xSessionId)    shouldBe Some(sessionId.value)
        request.header(HeaderNames.xRequestId)    shouldBe Some(requestId.value)
        request.header(HeaderNames.deviceID)      shouldBe Some(deviceID)
        request.header("path")                    shouldBe None
      }
    }
  }
}
