/*
 * Copyright 2015 HM Revenue & Customs
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
import uk.gov.hmrc.play.http.{Token, HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}

class ConnectorSpec extends WordSpecLike with Matchers {

  "AuthConnector.buildRequest" should {
    "add expected headers to the request" in running(FakeApplication()){
      val testAuthorisation = Authorization("someauth")
      val forwarded = ForwardedFor("forwarded")
      val token = Token("token")
      val sessionId = SessionId("session")
      val requestId = RequestId("requestId")

      val carrier: HeaderCarrier = HeaderCarrier(
        authorization = Some(testAuthorisation),
        token = Some(token),
        forwarded = Some(forwarded),
        sessionId = Some(sessionId),
        requestId = Some(requestId)
      )

      val request = new Connector {}.buildRequest("authBase")(carrier)
      request.headers.get(HeaderNames.authorisation).flatMap(_.headOption) shouldBe Some(testAuthorisation.value)
      request.headers.get(HeaderNames.xForwardedFor).flatMap(_.headOption) shouldBe Some(forwarded.value)
      request.headers.get(HeaderNames.token).flatMap(_.headOption) shouldBe Some(token.value)
      request.headers.get(HeaderNames.xSessionId).flatMap(_.headOption) shouldBe Some(sessionId.value)
      request.headers.get(HeaderNames.xRequestId).flatMap(_.headOption) shouldBe Some(requestId.value)
    }
  }
}
