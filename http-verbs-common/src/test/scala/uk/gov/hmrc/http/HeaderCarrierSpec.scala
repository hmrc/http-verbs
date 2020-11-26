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
import org.scalatest.LoneElement
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}

class HeaderCarrierSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with LoneElement {

  // TODO checks (especially `not contains`) should be case insensitive
  "headersForUrl" should {

   val internalUrls = List("http://test.public.service/bar", "http://test.public.mdtp/bar")
   val externalUrl  = "http://test.me"

    "should contain the values passed in by header-carrier for internal urls" in {
      val hc = HeaderCarrier(
        authorization = Some(Authorization("auth")),
        sessionId     = Some(SessionId("session")),
        requestId     = Some(RequestId("request")),
        forwarded     = Some(ForwardedFor("forwarded"))
      )

      internalUrls.map { url =>
        val result = hc.headersForUrl(HeaderCarrier.Config())(url)

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

      val result = hc.headersForUrl(HeaderCarrier.Config())(url = externalUrl)

      Seq(
        HeaderNames.authorisation,
        HeaderNames.xSessionId,
        HeaderNames.xRequestId,
        HeaderNames.xForwardedFor
      ).map(k => result.map(_._1) should not contain k)
    }

    "should include the User-Agent header when the 'appName' config value is present" in {
      val config = ConfigFactory.parseString(
        """|appName: myApp
           |internalServiceHostPatterns: [ "^.*\\.service$", "^.*\\.mdtp$" ]
           |bootstrap.http.headersAllowlist: []
           |""".stripMargin
      )

      (externalUrl :: internalUrls).map { url =>
        val result = HeaderCarrier().headersForUrl(HeaderCarrier.Config.fromConfig(config))(url)

        result should contain ("User-Agent" -> "myApp")
      }
    }

    "filter 'other headers' from request for external service calls" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val result = hc.headersForUrl(HeaderCarrier.Config())(url = externalUrl)

      result.map(_._1) should not contain "foo"
    }

    "filter 'other headers' in request for internal urls, if no allowlist provided" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      internalUrls.map { url =>
        val result = hc.headersForUrl(HeaderCarrier.Config())(url)
        result.map(_._1) should not contain "foo"
      }
    }


    "filter 'other headers' in request for internal urls, if not in provided allowlist" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = ConfigFactory.parseString(
        """|internalServiceHostPatterns: [ "^.*\\.service$", "^.*\\.mdtp$" ]
           |bootstrap.http.headersAllowlist: []
           |""".stripMargin
      )

      internalUrls.map { url =>
        val result = hc.headersForUrl(HeaderCarrier.Config.fromConfig(config))(url)
        result.map(_._1) should not contain "foo"
      }
    }

    "include 'remaining headers' in request for internal urls, if in provided allowlist" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = ConfigFactory.parseString(
        """|internalServiceHostPatterns: [ "^.*\\.service$", "^.*\\.mdtp$" ]
           |bootstrap.http.headersAllowlist: [foo]
           |""".stripMargin
      )

      internalUrls.map { url =>
        val result = hc.headersForUrl(HeaderCarrier.Config.fromConfig(config))(url)
        result should contain ("foo" -> "secret!")
      }
    }

    "include 'remaining headers' in request for internal service call to other configured internal URL pattern" in {
      val url = "http://localhost/foo" // an internal service call, according to config
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = ConfigFactory.parseString(
        """|internalServiceHostPatterns: [localhost]
           |bootstrap.http.headersAllowlist: [foo]
           |""".stripMargin
      )

      val result = hc.headersForUrl(HeaderCarrier.Config.fromConfig(config))(url)
      result should contain ("foo" -> "secret!")
    }
  }
}
