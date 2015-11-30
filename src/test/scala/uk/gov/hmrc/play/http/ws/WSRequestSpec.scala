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

package uk.gov.hmrc.play.http.ws

import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.Precondition._
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.play.http.{Precondition, HeaderCarrier, Token}

class WSRequestSpec extends WordSpecLike with Matchers {

  "buildRequest" should {

    "create a WSRequestBuilder that contains the values passed in by header-carrier" in
      running(FakeApplication()) {
        val url = "http://test.me"

        implicit val hc = HeaderCarrier(
          authorization = Some(Authorization("auth")),
          sessionId = Some(SessionId("session")),
          requestId = Some(RequestId("request")),
          token = Some(Token("token")),
          forwarded = Some(ForwardedFor("forwarded")))

        val wsRequest = new WSRequest {}
        val result = wsRequest.buildRequest(url, NoPrecondition)

        val expectedHeaders = hc.headers.map {
          case (a, b) => (a, List(b))
        }.toMap

        result.headers shouldBe expectedHeaders

        result.url shouldBe url
      }

    "create a WSRequestBuilder with a User-Agent header that has the 'appName' config value as its value" in
      running(FakeApplication(additionalConfiguration = Map("appName" -> "test-client"))) {

        val wsRequest = new WSRequest {}.buildRequest("http://test.me", NoPrecondition)(HeaderCarrier())

        wsRequest.headers("User-Agent").head shouldBe "test-client"
      }

    "create a WSRequestBuilder with an If-Match header that has the ifMatch precondition as its value" in
      running(FakeApplication()) {

        // https://tools.ietf.org/html/rfc7232#section-3.1
        val precondition = Precondition(ifMatch = Seq("\"xyzzy\"", "\"r2d2xxxx\"", "\"c3piozzzz\""))
        val wsRequest = new WSRequest {}.buildRequest("http://test.me", precondition)(HeaderCarrier())

        wsRequest.headers("If-Match").head shouldBe """"xyzzy", "r2d2xxxx", "c3piozzzz""""
      }

    "create a WSRequestBuilder with an If-None-Match header that has the ifNoneMatch precondition as its value" in
      running(FakeApplication()) {

        // https://tools.ietf.org/html/rfc7232#section-3.1
        val precondition = Precondition(ifNoneMatch = Seq("\"xyzzy\"", "\"r2d2xxxx\"", "\"c3piozzzz\""))
        val wsRequest = new WSRequest {}.buildRequest("http://test.me", precondition)(HeaderCarrier())

        wsRequest.headers("If-None-Match").head shouldBe """"xyzzy", "r2d2xxxx", "c3piozzzz""""
      }

  }

}
