/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.ws.WSProxyServer
import play.api.test.WsTestClient
import uk.gov.hmrc.http.test.{PortFinder, WireMockSupport}

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpClientImplSpec
  extends AnyWordSpecLike
     with Matchers
     with OptionValues
     with WireMockSupport
     with ScalaFutures
     with IntegrationPatience {

  private implicit val hc: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("x-test" -> "test-val"))

  WsTestClient.withClient { wsClient =>
    "HttpClientImpl.withUserAgent" should {
      val httpClient =
        new HttpClientImpl(
          configuration    = ConfigFactory.parseString("appName=myApp")
                              .withFallback(ConfigFactory.load()),
          hooks            = Seq.empty,
          wsClient         = wsClient,
          actorSystem      = ActorSystem("test-actor-system"),
          transformRequest = identity
        )

      "change user-agent" in {
        val userAgent = "new-user-agent"

        wireMockServer.stubFor(
          get(urlEqualTo("/"))
            .willReturn(aResponse().withStatus(200))
        )

        httpClient
          .withUserAgent(userAgent)
          .GET[HttpResponse](s"$wireMockUrl/")
          .futureValue

        wireMockServer.verify(
          getRequestedFor(urlEqualTo("/"))
            .withHeader("user-agent", equalTo(userAgent))
            .withHeader("x-test", equalTo("test-val"))
        )
      }
    }

    "HttpClientImpl.withProxy" should {
      val proxyProtocol = "http"
      val proxyHost     = "proxy.com"
      val proxyPort     = PortFinder.findFreePort(portRange = 6001 to 7000, excluded = wireMockPort)
      val proxyUsername = "u1"
      val proxyPassword = "p1"

      val proxyRef = new AtomicReference[Option[WSProxyServer]]

      val httpClient =
        new HttpClientImpl(
          configuration    = ConfigFactory
                               .parseString(
                                 s"""|proxy.enabled=true
                                     |proxy.protocol=$proxyProtocol
                                     |proxy.host=$proxyHost
                                     |proxy.port=$proxyPort
                                     |proxy.username=$proxyUsername
                                     |proxy.password=$proxyPassword
                                     |""".stripMargin
                               ).withFallback(ConfigFactory.load()),
          hooks            = Seq.empty,
          wsClient         = wsClient,
          actorSystem      = ActorSystem("test-actor-system"),
          transformRequest = identity
        )

      "apply proxy" in {
        httpClient
          .withProxy
          .withTransformRequest { req => proxyRef.set(req.proxyServer); req }
          .GET[HttpResponse](s"$wireMockUrl/")
          .failed.futureValue // it will fail since no proxy is running

        val proxyServer = proxyRef.get.value

        proxyServer.protocol  shouldBe Some(proxyProtocol)
        proxyServer.host      shouldBe proxyHost
        proxyServer.port      shouldBe proxyPort
        proxyServer.principal shouldBe Some(proxyUsername)
        proxyServer.password  shouldBe Some(proxyPassword)
      }
    }
  }
}
