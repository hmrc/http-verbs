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
import com.typesafe.config.Config
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import _root_.play.api.Application
import _root_.play.api.http.{HeaderNames => PlayHeaderNames}
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import _root_.play.api.libs.json.{JsValue, Json}
import _root_.play.api.libs.ws.WSClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.http.test.WireMockSupport

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HttpReads.Implicits._

class HeadersSpec
  extends AnyWordSpecLike
     with Matchers
     with WireMockSupport
     with ScalaFutures
     with IntegrationPatience {

  private lazy val app: Application = new GuiceApplicationBuilder().build()

  @silent("deprecated")
  private implicit val hc: HeaderCarrier = HeaderCarrier(
    authorization = Some(Authorization("authorization")),
    forwarded     = Some(ForwardedFor("forwarded-for")),
    sessionId     = Some(SessionId("session-id")),
    requestId     = Some(RequestId("request-id"))
  ).withExtraHeaders("extra-header" -> "my-extra-header")

  private lazy val httpClient =
    new HttpGet with HttpPost with HttpDelete with HttpPatch with HttpPut with WSHttp {
      override def wsClient: WSClient                 = app.injector.instanceOf[WSClient]
      override protected def configuration: Config    = app.configuration.underlying
      override val hooks: Seq[HttpHook]               = Seq.empty
      override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")
    }

  "a post request" when {
    "with an arbitrary body" should {
      "contain headers from the header carrier" in {
        stubFor(
          post(urlEqualTo("/arbitrary"))
            .willReturn(aResponse().withStatus(200))
        )

        httpClient
          .POST[JsValue, HttpResponse](s"$wireMockUrl/arbitrary", Json.obj())
          .futureValue

        verify(
          postRequestedFor(urlEqualTo("/arbitrary"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
        )
      }

      "allow a user to set an extra header in the POST" in {
        stubFor(
          post(urlEqualTo("/arbitrary"))
            .willReturn(aResponse().withStatus(200))
        )

        httpClient
          .POST[JsValue, HttpResponse](
            url     = s"$wireMockUrl/arbitrary",
            body    = Json.obj(),
            headers = Seq("extra-header-2" -> "my-extra-header-2")
          ).futureValue

        verify(
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
        stubFor(
          post(urlEqualTo("/string"))
            .willReturn(aResponse().withStatus(200))
        )

        httpClient
          .POSTString[HttpResponse](
            url  = s"$wireMockUrl/string",
            body = "foo"
          ).futureValue

        verify(
          postRequestedFor(urlEqualTo("/string"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
        )
      }

      "allow a user to set an extra header in the POST" in {
        stubFor(
          post(urlEqualTo("/string"))
            .willReturn(aResponse().withStatus(200))
        )

        httpClient
          .POSTString[HttpResponse](
            url     = s"$wireMockUrl/string",
            body    = "foo",
            headers = Seq("extra-header-2" -> "my-extra-header-2")
          ).futureValue

        verify(
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
        stubFor(
          post(urlEqualTo("/empty"))
            .willReturn(aResponse().withStatus(200))
        )

        httpClient
          .POSTEmpty[HttpResponse](s"$wireMockUrl/empty")
          .futureValue

        verify(
          postRequestedFor(urlEqualTo("/empty"))
            .withHeader(HeaderNames.authorisation, equalTo("authorization"))
            .withHeader(HeaderNames.xForwardedFor, equalTo("forwarded-for"))
            .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
            .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
            .withHeader("extra-header", equalTo("my-extra-header"))
            .withHeader(PlayHeaderNames.CONTENT_LENGTH, equalTo("0"))
        )
      }
    }
  }

  "a get request" should {
    "contain headers from the header carrier" in {
      stubFor(
        get(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      httpClient
        .GET[HttpResponse](s"$wireMockUrl/")
        .futureValue

      verify(
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
      wireMockServer.stubFor(
        delete(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      httpClient
        .DELETE[HttpResponse](
          url     = s"$wireMockUrl/",
          headers = Seq("header" -> "foo")
        ).futureValue

      wireMockServer.verify(
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
      stubFor(
        patch(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      httpClient
        .PATCH[JsValue, HttpResponse](
          url     = s"$wireMockUrl/",
          body    = Json.obj(),
          headers = Seq("header" -> "foo")
        )
        .futureValue

      verify(
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
      stubFor(
        put(urlEqualTo("/"))
          .willReturn(aResponse().withStatus(200))
      )

      httpClient
        .PUT[JsValue, HttpResponse](
          url  = s"$wireMockUrl/",
          body = Json.obj()
        ).futureValue

      verify(
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
