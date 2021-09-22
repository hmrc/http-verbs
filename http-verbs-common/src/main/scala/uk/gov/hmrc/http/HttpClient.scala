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
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.ws.{WSClient, WSProxyServer, WSRequest => PlayWSRequest}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch with RequestBuilder

trait RequestBuilder extends WSRequest {
  // implementations required to not break clients (which won't be using the new functions) e.g. implementations of ProxyHttpClient...
  def withUserAgent(userAgent: String): HttpClient =
    withPlayWSRequest { req =>
      req.withHttpHeaders("User-Agent" -> userAgent)
    }

  def withProxy: HttpClient = {
    val optProxyServer = WSProxyConfiguration.buildWsProxyServer(configuration)
    withPlayWSRequest { req =>
      optProxyServer.foldLeft(req)(_ withProxyServer _)
    }
  }

  def withPlayWSRequest(transform: PlayWSRequest => PlayWSRequest): HttpClient =
    sys.error("Your implementation of HttpClient does not implement `withBuildRequest`. Consider if you can use uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient") // TODO reference to bootstrap :(
}

class HttpClientImpl (
  override val configuration: Config,
  override val hooks        : Seq[HttpHook],
  override val wsClient     : WSClient,
  override val actorSystem  : ActorSystem
) extends HttpClient
     with WSHttp {

  import scala.collection.JavaConverters._

  // TODO HttpClientImpl should be final - any extension/overrides will be lost
  // as soon as `withUserAgent` or `withProxy` are called..
  override def withPlayWSRequest(transform: PlayWSRequest => PlayWSRequest): HttpClientImpl =
    new HttpClientImpl(
      configuration,
      hooks,
      wsClient,
      actorSystem
    ) {
      override def buildRequest[A](
        url    : String,
        headers: Seq[(String, String)]
      ): PlayWSRequest =
        transform(super.buildRequest(url, headers))
    }
}
