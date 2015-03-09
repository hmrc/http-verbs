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

package uk.gov.hmrc.play.audit.http

import org.scalatest.{Matchers, WordSpecLike}
import play.api.mvc.Session
import play.api.test.FakeHeaders
import uk.gov.hmrc.play.audit.http.HeaderCarrier.fromSessionAndHeaders
import uk.gov.hmrc.play.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.play.http.{HeaderNames, SessionKeys}

import scala.concurrent.duration._

class HeaderCarrierSpec extends WordSpecLike with Matchers {


  "Extracting the request timestamp from the session and headers" should {
    "find it in the header if present and a valid Long" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.xRequestTimestamp -> "12345")).nsStamp shouldBe 12345
    }

    "ignore it in the header if present but not a valid Long" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.xRequestTimestamp -> "13:14")).nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos
    }
  }

  "Extracting the forwarded information from the session and headers" should {
    "copy it from the X-Forwarded-For header if there is no True-Client-IP header" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.xForwardedFor -> "192.168.0.1")).forwarded shouldBe Some(ForwardedFor("192.168.0.1"))
    }

    "copy it from the X-Forwarded-For header if there is an empty True-Client-IP header" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.trueClientIp -> "", HeaderNames.xForwardedFor -> "192.168.0.1")).forwarded shouldBe Some(ForwardedFor("192.168.0.1"))
    }

    "be blank if the True-Client-IP header and the X-Forwarded-For header if both exist but are empty" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.trueClientIp -> "", HeaderNames.xForwardedFor -> "")).forwarded shouldBe Some(ForwardedFor(""))
    }

    "copy it from the True-Client-IP header if there is no X-Forwarded-For header" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.trueClientIp -> "192.168.0.1")).forwarded shouldBe Some(ForwardedFor("192.168.0.1"))
    }

    "merge the True-Client-IP header and the X-Forwarded-For header if both exist" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.trueClientIp -> "192.168.0.1", HeaderNames.xForwardedFor -> "192.168.0.2")).forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2"))
    }

    "do not add the True-Client-IP header if it's already at the beginning of X-Forwarded-For" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.trueClientIp -> "192.168.0.1", HeaderNames.xForwardedFor -> "192.168.0.1, 192.168.0.2")).forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2"))
    }

    "merge the True-Client-IP header if it already exists, but is not at the front ofthe X-Forwarded-For header" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.trueClientIp -> "192.168.0.1", HeaderNames.xForwardedFor -> "192.168.0.2, 192.168.0.1")).forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2, 192.168.0.1"))
    }
  }

  def headers(vals: (String, String)*) = FakeHeaders(vals.map { case (k, v) => k -> Seq(v)})

  "Extracting the remaining header carrier values from the session and headers" should {

    "find nothing with a blank request" in {
      val hc = fromSessionAndHeaders(Session(), FakeHeaders())
      hc.nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos
    }

    "find the userId from the session" in {
      fromSessionAndHeaders(Session(Map(SessionKeys.userId -> "beeblebrox")), headers()).userId shouldBe Some(UserId("beeblebrox"))
    }

    "find the token from the session" in {
      fromSessionAndHeaders(Session(Map(SessionKeys.token -> "THE_ONE_RING")), headers()).token shouldBe Some(Token("THE_ONE_RING"))
    }

    "find the authorization from the session" in {
      fromSessionAndHeaders(Session(Map(SessionKeys.authToken -> "let me in!")), headers()).authorization shouldBe Some(Authorization("let me in!"))
    }

    "find the requestId from the headers" in {
      fromSessionAndHeaders(Session(), headers(HeaderNames.xRequestId -> "18476239874162")).requestId shouldBe Some(RequestId("18476239874162"))
    }

    "find the sessionId from the session" in {
      fromSessionAndHeaders(Session(Map(SessionKeys.sessionId -> "sesssionIdFromSession")), headers()).sessionId shouldBe Some(SessionId("sesssionIdFromSession"))
    }

    "find the sessionId from the headers when not present in the session" in {
      fromSessionAndHeaders(Session(Map.empty), headers(HeaderNames.xSessionId -> "sessionIdFromHeader")).sessionId shouldBe Some(SessionId("sessionIdFromHeader"))
    }

    "ignore the sessionId when it is not present in the headers nor session" in {
      fromSessionAndHeaders(Session(Map.empty), headers()).sessionId shouldBe None
    }
  }
}
