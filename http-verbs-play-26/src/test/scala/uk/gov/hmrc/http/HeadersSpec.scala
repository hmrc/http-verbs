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
import com.github.ghik.silencer.silent
import com.github.tomakehurst.wiremock._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.Config
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.{Application, Play}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.{PortTester, WSHttp}

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HttpReads.Implicits._

class HeadersSpec
  extends AnyWordSpecLike
     with Matchers
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

  @silent("deprecated")
  private implicit val hc: HeaderCarrier = HeaderCarrier(
    authorization = Some(Authorization("authorization")),
    forwarded     = Some(ForwardedFor("forwarded-for")),
    sessionId     = Some(SessionId("session-id")),
    requestId     = Some(RequestId("request-id"))
  ).withExtraHeaders("extra-header" -> "my-extra-header")

  private lazy val client = new HttpGet with HttpPost with HttpDelete with HttpPatch with HttpPut with WSHttp {
    override def wsClient: WSClient                 = app.injector.instanceOf[WSClient]
    override protected def configuration: Config    = app.configuration.underlying
    override val hooks: Seq[HttpHook]               = Seq.empty
    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")
  }

  "a post request" when {
    "with an arbitrary body" should {
      "contain headers from the header carrier" in {
        server.stubFor(
          post(urlEqualTo("/arbitrary"))
            .willReturn(aResponse().withStatus(200))
        )

        client.POST[JsValue, HttpResponse](s"http://localhost:${server.port()}/arbitrary", Json.obj()).futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/arbitrary"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
        )
      }

      "allow a user to set an extra header in the POST" in {
        server.stubFor(
          post(urlEqualTo("/arbitrary"))
            .willReturn(aResponse().withStatus(200))
        )

        client.POST[JsValue, HttpResponse](
          url     = s"http://localhost:${server.port()}/arbitrary",
          body    = Json.obj(),
          headers = Seq("extra-header-2" -> "my-extra-header-2")
        ).futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/arbitrary"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
            .withHeader("extra-header-2", equalTo("my-extra-header-2"))
        )
      }
    }

    "with a string body" should {
      "contain headers from the header carrier" in {
        server.stubFor(
          post(urlEqualTo("/string"))
            .willReturn(aResponse().withStatus(200))
        )

        client.POSTString[HttpResponse](
          url  = s"http://localhost:${server.port()}/string",
          body = "foo"
        ).futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/string"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
        )
      }

      "allow a user to set an extra header in the POST" in {
        server.stubFor(
          post(urlEqualTo("/string"))
            .willReturn(aResponse().withStatus(200))
        )

        client.POSTString[HttpResponse](
          url     = s"http://localhost:${server.port()}/string",
          body    = "foo",
          headers = Seq("extra-header-2" -> "my-extra-header-2")
        ).futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/string"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
            .withHeader("extra-header-2", equalTo("my-extra-header-2"))
        )
      }
    }

    "with an empty body" should {
      "add a content length header if none is present" in {
        server.stubFor(
          post(urlEqualTo("/empty"))
            .willReturn(aResponse().withStatus(200))
        )

        client.POSTEmpty[HttpResponse](s"http://localhost:${server.port()}/empty").futureValue

        server.verify(
          postRequestedFor(urlEqualTo("/empty"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
            .withHeader(play.api.http.HeaderNames.CONTENT_LENGTH, equalTo("0"))
        )
      }
    }
  }

  "a get request" should {
    "contain headers from the header carrier" in {
      server.stubFor(
        get(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      client
        .GET[HttpResponse](s"http://localhost:${server.port()}/")
        .futureValue

      server.verify(
        getRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header"))
      )
    }
  }

  "a delete request" should {
    "contain headers from the header carrier" in {
      server.stubFor(
        delete(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      client.DELETE[HttpResponse](
        url     = s"http://localhost:${server.port()}/",
        headers = Seq("header" -> "foo")
      ).futureValue

      server.verify(
        deleteRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header"))
          .withHeader("header", equalTo("foo"))
      )
    }
  }

  "a patch request" should {
    "contain headers from the header carrier" in {
      server.stubFor(
        patch(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      client
        .PATCH[JsValue, HttpResponse](
          url     = s"http://localhost:${server.port()}/",
          body    = Json.obj(),
          headers = Seq("header" -> "foo")
        )
        .futureValue

      server.verify(
        patchRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header"))
          .withHeader("header", equalTo("foo"))
      )
    }
  }

  "a put request" should {
    "contain headers from the header carrier" in {
      server.stubFor(
        put(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      client.PUT[JsValue, HttpResponse](
        url  = s"http://localhost:${server.port()}/",
        body = Json.obj()
      ).futureValue

      server.verify(
        putRequestedFor(urlEqualTo("/"))
          .withHeader(HeaderNames.authorisation, equalTo("authorization"))
          .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader("extra-header", equalTo("my-extra-header"))
      )
    }
  }
}
