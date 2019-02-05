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

package uk.gov.hmrc.http

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.Config
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FreeSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.{Application, Play}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.logging.{Authorization, ForwardedFor, RequestId, SessionId}
import uk.gov.hmrc.play.http.ws.{PortTester, WSHttp}

import scala.concurrent.ExecutionContext.Implicits.global

class HeadersSpec
    extends FreeSpec
    with MustMatchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application = new GuiceApplicationBuilder().build()
  private val server                = new WireMockServer(wireMockConfig().port(PortTester.findPort()))

  override def beforeAll(): Unit = {
    Play.start(app)
    server.start()
  }

  override def afterAll(): Unit = {
    server.stop()
    Play.stop(app)
  }

  override def beforeEach(): Unit = {
    server.stop()
    server.start()
  }

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier(
    authorization = Some(Authorization("authorization")),
    forwarded     = Some(ForwardedFor("forwarded-for")),
    sessionId     = Some(SessionId("session-id")),
    requestId     = Some(RequestId("request-id"))
  ).withExtraHeaders("extra-header" -> "my-extra-header")

  private lazy val client = new HttpGet with HttpPost with HttpDelete with HttpPatch with HttpPut with WSHttp {
    override def wsClient: WSClient                      = app.injector.instanceOf[WSClient]
    override protected def configuration: Option[Config] = None
    override val hooks: Seq[HttpHook]                    = Seq.empty
    override protected def actorSystem: ActorSystem      = ActorSystem("test-actor-system")
  }

  "a post request" - {

    "with an arbtirary body" - {

      "must contain headers from the header carrier" in {

        server.stubFor(
          post(urlEqualTo("/arbitrary"))
            .willReturn(aResponse().withStatus(200)))

        client.POST[JsValue, HttpResponse](s"http://localhost:${server.port()}/arbitrary", Json.obj()).futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/arbitrary"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header")))
      }
    }

    "with a string body" - {

      "must contain headers from the header carrier" in {

        server.stubFor(
          post(urlEqualTo("/string"))
            .willReturn(aResponse().withStatus(200)))

        client.POSTString[HttpResponse](s"http://localhost:${server.port()}/string", "foo").futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/string"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header")))
      }
    }
  }

  "a get request" - {

    "must contain headers from the header carrier" in {

      server.stubFor(
        get(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200)))

      client.GET[HttpResponse](s"http://localhost:${server.port()}/").futureValue

      server.verify(
        getRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header")))
    }
  }

  "a delete request" - {

    "must contain headers from the header carrier" in {

      server.stubFor(
        delete(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200)))

      client.DELETE[HttpResponse](s"http://localhost:${server.port()}/").futureValue

      server.verify(
        deleteRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header")))
    }
  }

  "a patch request" - {

    "must contain headers from the header carrier" in {

      server.stubFor(
        patch(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200)))

      client.PATCH[JsValue, HttpResponse](s"http://localhost:${server.port()}/", Json.obj()).futureValue

      server.verify(
        patchRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header")))
    }
  }

  "a put request" - {

    "must contain headers from the header carrier" in {

      server.stubFor(
        put(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200)))

      client.PUT[JsValue, HttpResponse](s"http://localhost:${server.port()}/", Json.obj()).futureValue

      server.verify(
        putRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header")))
    }
  }
}
