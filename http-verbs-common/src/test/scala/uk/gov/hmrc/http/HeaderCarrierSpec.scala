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

package uk.gov.hmrc.http

import java.util

import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalactic.Explicitly._
import org.scalactic.StringNormalizations.lowerCased
import org.scalatest.LoneElement
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class HeaderCarrierSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with LoneElement {

  "headersForUrl" should {

   val internalUrls = List(
     "http://test.public.service/bar",
     "http://test.public.mdtp/bar",
     "http://localhost:1234/bar"
   )
   val externalUrl  = "http://test.me"

   def mkConfig(s: String = ""): HeaderCarrier.Config =
     HeaderCarrier.Config.fromConfig(
       ConfigFactory.parseString(s)
         .withFallback(ConfigFactory.load())
     )

    "should contain the values passed in by header-carrier for internal urls" in {
      val hc = HeaderCarrier(
        authorization = Some(Authorization("auth")),
        sessionId     = Some(SessionId("session")),
        requestId     = Some(RequestId("request")),
        forwarded     = Some(ForwardedFor("forwarded"))
      )

      internalUrls.map { url =>
        val result = hc.headersForUrl(mkConfig())(url)

        Seq(
          HeaderNames.authorisation -> "auth",
          HeaderNames.xSessionId    -> "session",
          HeaderNames.xRequestId    -> "request",
          HeaderNames.xForwardedFor -> "forwarded"
        ).map(hdr => result should contain (hdr))
      }
    }

    "should not contain the values passed in by header-carrier for external urls, if no config" in {
      val hc = HeaderCarrier(
        authorization = Some(Authorization("auth")),
        sessionId     = Some(SessionId("session")),
        requestId     = Some(RequestId("request")),
        forwarded     = Some(ForwardedFor("forwarded"))
      )

      val result = hc.headersForUrl(mkConfig())(url = externalUrl)

      Seq(
        HeaderNames.authorisation,
        HeaderNames.xSessionId,
        HeaderNames.xRequestId,
        HeaderNames.xForwardedFor
      ).map(k => (result.map(_._1) should not contain k) (after being lowerCased))
    }

    "should include the User-Agent header when the 'appName' config value is present" in {
      val config = mkConfig("appName: myApp")

      (externalUrl :: internalUrls).map { url =>
        val result = HeaderCarrier().headersForUrl(config)(url)

        result should contain ("User-Agent" -> "myApp")
      }
    }

    "filter 'other headers' from request for external service calls" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val result = hc.headersForUrl(mkConfig())(url = externalUrl)

      (result.map(_._1) should not contain "foo") (after being lowerCased)
    }

    "filter 'other headers' in request for internal urls, if no allowlist provided" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      internalUrls.map { url =>
        val result = hc.headersForUrl(mkConfig())(url)
        (result.map(_._1) should not contain "foo") (after being lowerCased)
      }
    }

    "filter 'other headers' in request for internal urls, if not in provided allowlist" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = mkConfig("bootstrap.http.headersAllowlist: []")

      internalUrls.map { url =>
        val result = hc.headersForUrl(config)(url)
        (result.map(_._1) should not contain "foo") (after being lowerCased)
      }
    }

    "include 'remaining headers' in request for internal urls, if in provided allowlist" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config =
        mkConfig("bootstrap.http.headersAllowlist: [foo]")

      internalUrls.map { url =>
        val result = hc.headersForUrl(config)(url)
        result should contain ("foo" -> "secret!")
      }
    }

    "include 'remaining headers' in request for internal service call to other configured internal URL pattern" in {
      val url = "http://localhost/foo" // an internal service call, according to config
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = mkConfig("bootstrap.http.headersAllowlist: [foo]")

      val result = hc.headersForUrl(config)(url)
      result should contain ("foo" -> "secret!")
    }
  }
}
