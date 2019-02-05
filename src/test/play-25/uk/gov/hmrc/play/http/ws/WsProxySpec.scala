/*
 * Copyright 2019 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpecLike}
import play.api.Play
import play.api.libs.ws.WSProxyServer
import play.api.test.FakeApplication
import uk.gov.hmrc.http.HeaderCarrier

class WsProxySpec extends WordSpecLike with Matchers with MockitoSugar with OptionValues with BeforeAndAfterAll {
  implicit val hc = HeaderCarrier()

  lazy val fakeApplication = FakeApplication()

  "A proxied get request" should {
    "correctly make a request via the specified proxy server" in new Setup {

      val wSProxyServer = mock[WSProxyServer]

      object ProxiedGet extends WSProxy {

        override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Nil

        def wsProxyServer = Some(wSProxyServer)
      }

      val request = ProxiedGet.buildRequest("http://example.com")

      request.proxyServer.value shouldBe (wSProxyServer)
    }
  }

  "A proxied get request, without a defined proxy configuration, i.e. for use in environments where a proxy does not exist" should {
    "still work by making the request without using a proxy server" in new Setup {

      object ProxiedGet extends WSProxy {

        override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Nil

        def wsProxyServer = None
      }

      val request = ProxiedGet.buildRequest("http://example.com")

      request.proxyServer shouldBe (None)
    }
  }

  class Setup extends WireMockEndpoints {

    val responseData = "ResourceABC"
    val resourcePath = s"/resource/abc"

    def endpointBaseUrl = s"http://localhost:$endpointPort"

    def fullResourceUrl = s"$endpointBaseUrl$resourcePath"

    def setupEndpointExpectations() {
      endpointMock.register(
        get(urlEqualTo(resourcePath))
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "text/plain")
              .withBody(responseData)))

      proxyMock.register(
        get(urlMatching(resourcePath))
          .willReturn(aResponse().proxiedFrom(endpointBaseUrl)))
    }

    def assertEndpointWasCalled() {
      endpointMock.verifyThat(getRequestedFor(urlEqualTo(resourcePath)))
    }

    def assertCallViaProxy() {
      endpointMock.verifyThat(getRequestedFor(urlEqualTo(resourcePath)))
    }

    def assertProxyNotUsed() {
      a[VerificationException] should be thrownBy proxyMock.verifyThat(getRequestedFor(urlEqualTo(resourcePath)))
    }
  }

  override def beforeAll() {
    super.beforeAll()
    Play.start(fakeApplication)
  }

  override def afterAll() {
    super.afterAll()
    Play.stop(fakeApplication)
  }

}
