package uk.gov.hmrc.play.http.ws

import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.ws.DefaultWSProxyServer
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.play.test.UnitSpec


class WsProxySpec extends UnitSpec with WithFakeApplication {

  implicit val hc = HeaderCarrier()

  "A proxied get request" should {
    "correctly make a request via the specified proxy server" in new Setup {

      object ProxiedGet extends WSGet with WSProxy {
        def wsProxyServer = Some(DefaultWSProxyServer(host = host, port = proxyPort))
      }

      withServers {
        setupEndpointExpectations()

        val responseFuture = ProxiedGet.doGet(fullResourceUrl)

        await(responseFuture).body shouldBe responseData

        assertEndpointWasCalled()
        assertCallViaProxy()
      }
    }
  }

  "A proxied get request, without a defined proxy configuration, i.e. for use in environments where a proxy does not exist" should {
    "still work by making the request without using a proxy server" in new Setup {

      object ProxiedGet extends WSGet with WSProxy {
        def wsProxyServer = None
      }

      withServers {
        setupEndpointExpectations()

        val responseFuture = ProxiedGet.doGet(fullResourceUrl)

        await(responseFuture).body shouldBe responseData

        assertEndpointWasCalled()
        assertProxyNotUsed()
      }
    }
  }

  "A non-proxied get request" should {
    "not make a request via the proxy server" in new Setup {

      object NonProxiedGet extends WSGet

      withServers {
        setupEndpointExpectations()

        val responseFuture = NonProxiedGet.doGet(fullResourceUrl)

        await(responseFuture).body shouldBe responseData

        assertEndpointWasCalled()
        assertProxyNotUsed()
      }
    }
  }

  class Setup extends WireMockEndpoints {

    val responseData = "ResourceABC"
    val resourcePath = s"/resource/abc"

    def endpointBaseUrl = s"http://localhost:$endpointPort"

    def fullResourceUrl = s"$endpointBaseUrl$resourcePath"

    def setupEndpointExpectations() {
      endpointMock.register(get(urlEqualTo(resourcePath))
        .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBody(responseData)))

      proxyMock.register(get(urlMatching(resourcePath))
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
}
