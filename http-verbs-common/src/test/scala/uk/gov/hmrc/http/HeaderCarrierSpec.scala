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

  "headersForUrl" should {

    "should contain the values passed in by header-carrier" in {
      val hc = HeaderCarrier(
        authorization = Some(Authorization("auth")),
        sessionId     = Some(SessionId("session")),
        requestId     = Some(RequestId("request")),
        forwarded     = Some(ForwardedFor("forwarded"))
      )

      val result = hc.headersForUrl(config = None)(url = "http://test.me")

      Seq(
        HeaderNames.authorisation -> "auth",
        HeaderNames.xSessionId    -> "session",
        HeaderNames.xRequestId    -> "request",
        HeaderNames.xForwardedFor -> "forwarded"
      ).map(hdr => result should contain (hdr))
    }

    "should include the User-Agent header when the 'appName' config value is present" in {
      val config = ConfigFactory.parseString(
        """|appName: myApp
           |bootstrap.http.headersAllowlist: []
           |""".stripMargin
      )

      val result = HeaderCarrier().headersForUrl(config = Some(config))("http://test.me")

      result should contain ("User-Agent" -> "myApp")
    }

    "filter 'other headers' from request for external service calls" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val result = hc.headersForUrl(config = None)(url = "http://test.me")

      result.map(_._1) should not contain "foo"
    }

    "filter 'other headers' in request for internal service call to .service URL, if no allowlist provided" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      List("http://test.public.service/bar", "http://test.public.mdtp/bar").map { url =>
        val result = hc.headersForUrl(config = None)(url)
        result.map(_._1) should not contain "foo"
      }
    }


    "filter 'other headers' in request for internal service call to .service URL, if not in provided allowlist" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = ConfigFactory.parseString(
        """|bootstrap.http.headersAllowlist: []
           |""".stripMargin
      )

      List("http://test.public.service/bar", "http://test.public.mdtp/bar").map { url =>
        val result = hc.headersForUrl(config = Some(config))(url)
        result.map(_._1) should not contain "foo"
      }
    }

    "include 'remaining headers' in request for internal service call to .service URL, if in provided allowlist" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = ConfigFactory.parseString(
        """|bootstrap.http.headersAllowlist: [foo]
           |""".stripMargin
      )

      List("http://test.public.service/bar", "http://test.public.mdtp/bar").map { url =>
        val result = hc.headersForUrl(config = Some(config))(url)
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

      val result = hc.headersForUrl(config = Some(config))(url)
      result should contain ("foo" -> "secret!")
    }
  }
}

/*

    "work if httpHeadersAllowlist not provided in config" in {
      val headerCarrierConverter = new HeaderCarrierConverter {
        protected def configuration: Configuration = Configuration.empty
      }

      headerCarrierConverter.fromHeadersAndSession(headers()).otherHeaders shouldBe Seq()
    }


    "add all remaining headers as other headers, ignoring explicit ones" in running(
      FakeApplication(additionalConfiguration = Map("bootstrap.http.headersAllowlist" -> Seq("quix")))) {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(HeaderNames.xRequestId -> "18476239874162", "User-Agent" -> "quix", "quix" -> "foo"),
          Some(Session()))
        .otherHeaders shouldBe Seq("quix" -> "foo")
    }

    "allowlisted headers check should be case insensitive" in running(
      FakeApplication(additionalConfiguration = Map("bootstrap.http.headersAllowlist" -> Seq("x-client-id")))) {
      HeaderCarrierConverter
        .fromHeadersAndSession(
          headers(HeaderNames.xRequestId -> "18476239874162", "User-Agent" -> "quix", "X-Client-ID" -> "foo"),
          Some(Session()))
        .otherHeaders shouldBe Seq("X-Client-ID" -> "foo")
    }
*/