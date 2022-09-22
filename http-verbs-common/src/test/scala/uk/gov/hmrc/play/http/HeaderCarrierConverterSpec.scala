/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.play.http

import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Configuration, Play}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.http.{HeaderNames => PlayHeaderNames}
import play.api.test.{FakeHeaders, FakeRequest, FakeRequestFactory}
import play.api.mvc.request.RequestFactory
import uk.gov.hmrc.http._

import scala.concurrent.duration._

@annotation.nowarn("msg=deprecated")
class HeaderCarrierConverterSpec extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  "Extracting the request timestamp from the session and headers" should {
    "find it in the header if present and a valid Long" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.xRequestTimestamp -> "12345"), Some(Session()))
        .nsStamp shouldBe 12345

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.xRequestTimestamp -> "12345"),
          session = Session()
        )
        .nsStamp shouldBe 12345
    }

    "ignore it in the header if present but not a valid Long" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.xRequestTimestamp -> "13:14"), Some(Session()))
        .nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.xRequestTimestamp -> "13:14"),
          session = Session()
        )
        .nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos
    }
  }

  "Extracting the forwarded information from the session and headers" should {
    "copy it from the X-Forwarded-For header if there is no True-Client-IP header" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.xForwardedFor -> "192.168.0.1"), Some(Session()))
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.xForwardedFor -> "192.168.0.1"),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1"))
    }

    "copy it from the X-Forwarded-For header if there is an empty True-Client-IP header" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(
            HeaderNames.trueClientIp  -> "",
            HeaderNames.xForwardedFor -> "192.168.0.1"
          ),
          Some(Session()))
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.trueClientIp  -> "",
                      HeaderNames.xForwardedFor -> "192.168.0.1"
                    ),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1"))
    }

    "be blank if the True-Client-IP header and the X-Forwarded-For header if both exist but are empty" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(
            HeaderNames.trueClientIp  -> "",
            HeaderNames.xForwardedFor -> ""
          ),
          Some(Session()))
        .forwarded shouldBe Some(ForwardedFor(""))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.trueClientIp  -> "",
                      HeaderNames.xForwardedFor -> ""
                    ),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor(""))
    }

    "copy it from the True-Client-IP header if there is no X-Forwarded-For header" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.trueClientIp -> "192.168.0.1"), Some(Session()))
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.trueClientIp -> "192.168.0.1"),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1"))
    }

    "merge the True-Client-IP header and the X-Forwarded-For header if both exist" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(
            HeaderNames.trueClientIp  -> "192.168.0.1",
            HeaderNames.xForwardedFor -> "192.168.0.2"
          ),
          Some(Session()))
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.trueClientIp  -> "192.168.0.1",
                      HeaderNames.xForwardedFor -> "192.168.0.2"
                    ),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2"))
    }

    "do not add the True-Client-IP header if it's already at the beginning of X-Forwarded-For" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(
            HeaderNames.trueClientIp  -> "192.168.0.1",
            HeaderNames.xForwardedFor -> "192.168.0.1, 192.168.0.2"
          ),
          Some(Session()))
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.trueClientIp  -> "192.168.0.1",
                      HeaderNames.xForwardedFor -> "192.168.0.1, 192.168.0.2"
                    ),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2"))
    }

    "merge the True-Client-IP header if it already exists, but is not at the front ofthe X-Forwarded-For header" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(
            HeaderNames.trueClientIp  -> "192.168.0.1",
            HeaderNames.xForwardedFor -> "192.168.0.2, 192.168.0.1"
          ),
          Some(Session()))
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2, 192.168.0.1"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.trueClientIp  -> "192.168.0.1",
                      HeaderNames.xForwardedFor -> "192.168.0.2, 192.168.0.1"
                    ),
          session = Session()
        )
        .forwarded shouldBe Some(ForwardedFor("192.168.0.1, 192.168.0.2, 192.168.0.1"))
    }
  }

  "Extracting the remaining header carrier values from the session and headers" should {
    "find nothing with a blank request" in {
      val hc = HeaderCarrierConverter.fromHeadersAndSession(FakeHeaders())
      hc.nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos

      HeaderCarrierConverter
        .fromRequest(FakeRequest())
        .nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos

      HeaderCarrierConverter
        .fromRequestAndSession(FakeRequest(), Session())
        .nsStamp shouldBe System.nanoTime() +- 5.seconds.toNanos
    }

    "find the authorization from the session" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(), Some(Session(Map(SessionKeys.authToken -> "let me in!"))))
        .authorization shouldBe Some(Authorization("let me in!"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest(),
          session = Session(Map(SessionKeys.authToken -> "let me in!"))
        )
        .authorization shouldBe Some(Authorization("let me in!"))
    }

    "find the requestId from the headers" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.xRequestId -> "18476239874162"), Some(Session()))
        .requestId shouldBe Some(RequestId("18476239874162"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.xRequestId -> "18476239874162"),
          session = Session()
        )
        .requestId shouldBe Some(RequestId("18476239874162"))
    }

    "find the sessionId from the session" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(), Some(Session(Map(SessionKeys.sessionId -> "sesssionIdFromSession"))))
        .sessionId shouldBe Some(SessionId("sesssionIdFromSession"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest(),
          session = Session(Map(SessionKeys.sessionId -> "sesssionIdFromSession"))
        )
        .sessionId shouldBe Some(SessionId("sesssionIdFromSession"))
    }

    "find the sessionId from the headers when not present in the session" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.xSessionId -> "sessionIdFromHeader"), Some(Session(Map.empty)))
        .sessionId shouldBe Some(SessionId("sessionIdFromHeader"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.xSessionId -> "sessionIdFromHeader"),
          session = Session(Map.empty)
        )
        .sessionId shouldBe Some(SessionId("sessionIdFromHeader"))
    }

    "ignore the sessionId when it is not present in the headers nor session" in {
      HeaderCarrierConverter.fromHeadersAndSession(headers(), Some(Session(Map.empty))).sessionId shouldBe None

      HeaderCarrierConverter
        .fromRequestAndSession(FakeRequest(), Session())
        .sessionId shouldBe None
    }

    "find the akamai reputation from the headers" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(HeaderNames.akamaiReputation -> "ID=127.0.0.1;WEBATCK=7"),
          Some(Session())
        )
        .akamaiReputation shouldBe Some(AkamaiReputation("ID=127.0.0.1;WEBATCK=7"))

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(HeaderNames.akamaiReputation -> "ID=127.0.0.1;WEBATCK=7"),
          session = Session()
        )
        .akamaiReputation shouldBe Some(AkamaiReputation("ID=127.0.0.1;WEBATCK=7"))
    }

    "add all remaining headers as other headers" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(
            HeaderNames.xRequestId -> "18476239874162",
            "User-Agent"           -> "quix",
            "quix"                 -> "foo"
          )
        )
        .otherHeaders shouldBe Seq(
          "User-Agent" -> "quix",
          "quix"       -> "foo"
        )

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.xRequestId -> "18476239874162",
                      "User-Agent"           -> "quix",
                      "quix"                 -> "foo"
                    ),
          session = Session()
        )
        .otherHeaders.toSet should contain allElementsOf (Set(
          "User-Agent" -> "quix",
          "quix"       -> "foo",
          "path"       -> "/"
        ))

      HeaderCarrierConverter
        .fromRequest(
          request = FakeRequest().withHeaders(
                      HeaderNames.xRequestId -> "18476239874162",
                      "User-Agent"           -> "quix",
                      "quix"                 -> "foo"
                    )
        )
        .otherHeaders.toSet should contain allElementsOf (Set(
          "User-Agent" -> "quix",
          "quix"       -> "foo",
          "path"       -> "/"
        ))
    }

    "add the request path" in {
      HeaderCarrierConverter
        .fromHeadersAndSessionAndRequest(
          headers(),
          request = Some(FakeRequest(GET, path = "/the/request/path"))
        )
        .otherHeaders shouldBe Seq("path" -> "/the/request/path")

      HeaderCarrierConverter
        .fromRequestAndSession(
          request = FakeRequest(GET, path = "/the/request/path"),
          session = Session()
        )
        .otherHeaders should contain ("path" -> "/the/request/path")

      HeaderCarrierConverter
        .fromRequest(
          request = FakeRequest(GET, path = "/the/request/path")
        )
        .otherHeaders should contain ("path" -> "/the/request/path")
    }
  }

  "build Google Analytics headers from request" should {
    "find the GA user id and token" in {
      {
        val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(
          headers(
            HeaderNames.googleAnalyticTokenId -> "ga-token",
            HeaderNames.googleAnalyticUserId  -> "123.456"
          )
        )
        hc.gaToken  shouldBe Some("ga-token")
        hc.gaUserId shouldBe Some("123.456")
      }

      {
        val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(
          request = FakeRequest().withHeaders(
                      HeaderNames.googleAnalyticTokenId -> "ga-token",
                      HeaderNames.googleAnalyticUserId  -> "123.456"
                    ),
          session = Session()
        )
        hc.gaToken  shouldBe Some("ga-token")
        hc.gaUserId shouldBe Some("123.456")
      }
    }
  }

  "utilise values from cookies" should {
    "find the deviceID from the cookie if set in header" in {
      val cookieHeader = Cookies.encodeCookieHeader(Seq(Cookie(CookieNames.deviceID, "deviceIdCookie")))
      val req          = FakeRequest().withHeaders(PlayHeaderNames.COOKIE -> cookieHeader)

      HeaderCarrierConverter.fromHeadersAndSession(req.headers, Some(req.session))
        .deviceID shouldBe Some("deviceIdCookie")
    }

    "find the deviceID from the cookie if set in the session" in {
      val cookie  = Cookie(CookieNames.deviceID, "deviceIdCookie")
      val req     = FakeRequest().withCookies(cookie)

      HeaderCarrierConverter.fromHeadersAndSessionAndRequest(req.headers, Some(req.session), Some(req))
        .deviceID shouldBe Some("deviceIdCookie")
    }

    "find the deviceID from the headers if the cookie is not set such as in an internal microservice call" in {
      HeaderCarrierConverter
        .fromHeadersAndSession(headers(HeaderNames.deviceID -> "deviceIdTest"), Some(Session(Map.empty)))
        .deviceID shouldBe Some("deviceIdTest")
    }

    "find the deviceID from the headers if the cookie and session are not available" in {
      // create a request with no extra session or attributes
      val req = new FakeRequestFactory(RequestFactory.plain).apply()
      HeaderCarrierConverter
        .fromHeadersAndSessionAndRequest(headers(HeaderNames.deviceID -> "deviceIdTest"), None, Some(req))
        .deviceID shouldBe Some("deviceIdTest")
    }
  }

  def headers(vals: (String, String)*) = FakeHeaders(vals.map { case (k, v) => k -> v })

  lazy val fakeApplication =
    GuiceApplicationBuilder(configuration = Configuration("play.allowGlobalApplication" -> true)).build()

  override def beforeAll() {
    super.beforeAll()
    Play.start(fakeApplication)
  }

  override def afterAll() {
    super.afterAll()
    Play.stop(fakeApplication)
  }
}
