/*
 * Copyright 2017 HM Revenue & Customs
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

import com.typesafe.config.Config
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{LoneElement, Matchers, WordSpecLike}
import uk.gov.hmrc.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}


class HttpRequestSpec extends WordSpecLike with Matchers with MockitoSugar with LoneElement {

  "applicableHeaders" should {

    "should contains the values passed in by header-carrier" in {
      val url = "http://test.me"

      implicit val hc = HeaderCarrier(
        authorization = Some(Authorization("auth")),
        sessionId = Some(SessionId("session")),
        requestId = Some(RequestId("request")),
        token = Some(Token("token")),
        forwarded = Some(ForwardedFor("forwarded")))

      val httpRequest = new HttpRequest {
        override def configuration: Option[Config] = None
      }
      val result = httpRequest.applicableHeaders(url)

      result shouldBe hc.headers
    }

    "should include the User-Agent header when the 'appName' config value is present" in {

      val mockedConfig = mock[Config]
      when(mockedConfig.getStringList(any())).thenReturn(new util.ArrayList[String]())
      when(mockedConfig.getString("appName")).thenReturn("myApp")
      when(mockedConfig.hasPathOrNull("appName")).thenReturn(true)

      val httpRequest = new HttpRequest {
        override def configuration: Option[Config] = Some(mockedConfig)
      }
      val result = httpRequest.applicableHeaders("http://test.me")(HeaderCarrier())

      result.contains("User-Agent" -> "myApp") shouldBe true
    }
    
    "filter 'remaining headers' from request for external service calls" in {

      implicit val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      val httpRequest = new HttpRequest {
        override def configuration: Option[Config] = None
      }
      val result = httpRequest.applicableHeaders("http://test.me")
      result.map(_._1) should not contain "foo"
    }

    "include 'remaining headers' in request for internal service call to .service URL" in {
      implicit val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )
      val httpRequest = new HttpRequest {
        override def configuration: Option[Config] = None
      }

      for {url <- List("http://test.public.service/bar", "http://test.public.mdtp/bar"  )} {

        val result = httpRequest.applicableHeaders(url)
        assert(result.contains("foo" -> "secret!"), s"'other/remaining headers' for $url were not present")

      }
    }

    "include 'remaining headers' in request for internal service call to other configured internal URL pattern" in {
      val url = "http://localhost/foo" // an internal service call, according to config
      implicit val hc = HeaderCarrier(
        otherHeaders = Seq("foo" -> "secret!")
      )

      import scala.collection.JavaConversions._
      val mockedConfig = mock[Config]
      when(mockedConfig.getStringList("internalServiceHostPatterns")).thenReturn(List("localhost"))
      when(mockedConfig.hasPathOrNull("internalServiceHostPatterns")).thenReturn(true)

      val httpRequest = new HttpRequest {
        override def configuration: Option[Config] = Some(mockedConfig)
      }
      val result = httpRequest.applicableHeaders(url)
      result.contains("foo" -> "secret!") shouldBe true
    }

  }

}
