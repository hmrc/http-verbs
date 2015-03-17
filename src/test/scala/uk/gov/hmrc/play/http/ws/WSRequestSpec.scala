/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http.ws

import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, Token}
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}

class WSRequestSpec extends WordSpecLike with Matchers {

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
