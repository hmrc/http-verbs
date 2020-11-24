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
      val config = ConfigFactory.parseString("appName: myApp")

      val result = HeaderCarrier().headersForUrl(config = Some(config))("http://test.me")

      result should contain ("User-Agent" -> "myApp")
    }

    "filter 'remaining headers' from request for external service calls" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val result = hc.headersForUrl(config = None)(url = "http://test.me")

      result.map(_._1) should not contain "foo"
    }

    "include 'remaining headers' in request for internal service call to .service URL" in {
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      List("http://test.public.service/bar", "http://test.public.mdtp/bar").map { url =>
        val result = hc.headersForUrl(config = None)(url)
        result should contain ("foo" -> "secret!")
      }
    }

    "include 'remaining headers' in request for internal service call to other configured internal URL pattern" in {
      val url = "http://localhost/foo" // an internal service call, according to config
      val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val config = ConfigFactory.parseString("internalServiceHostPatterns: [localhost]")

      val result = hc.headersForUrl(config = Some(config))(url)
      result should contain ("foo" -> "secret!")
    }
  }
}
