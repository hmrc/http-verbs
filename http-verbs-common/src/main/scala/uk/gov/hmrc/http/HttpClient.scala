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
import com.typesafe.config.Config
import play.api.libs.ws.{WSClient, WSRequest => PlayWSRequest}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch with WSRequestBuilder {

  private def replaceHeader(req: PlayWSRequest, header: (String, String)): PlayWSRequest = {
    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(v => k -> v) }
    val hdrsWithoutKey = req.headers.filterKeys(!_.equalsIgnoreCase(header._1)) // replace existing header
    req.withHttpHeaders(denormalise(hdrsWithoutKey) :+ header : _*)
  }

  def withUserAgent(userAgent: String): HttpClient =
    withTransformRequest { req =>
      replaceHeader(req, "User-Agent" -> userAgent)
    }

  def withProxy: HttpClient = {
    val optProxyServer = WSProxyConfiguration.buildWsProxyServer(configuration)
    withTransformRequest { req =>
      optProxyServer.foldLeft(req)(_ withProxyServer _)
    }
  }

  // implementation required to not break clients (which won't be using the new functions)
  // e.g. implementations of ProxyHttpClient, and the deprecated DefaultHttpClient in bootstrap.
  def withTransformRequest(transform: PlayWSRequest => PlayWSRequest): HttpClient =
    sys.error("Your implementation of HttpClient does not implement `withTransformRequest`. You can use uk.gov.hmrc.http.HttpClientImpl")
}

// class is final, since any overrides would be lost in the result of `withPlayWSRequest`
final class HttpClientImpl (
  override val configuration   : Config,
  override val hooks           : Seq[HttpHook],
  override val wsClient        : WSClient,
  override val actorSystem     : ActorSystem,
  override val transformRequest: PlayWSRequest => PlayWSRequest
) extends HttpClient
     with WSHttp {

  override def withTransformRequest(transformRequest: PlayWSRequest => PlayWSRequest): HttpClientImpl =
    new HttpClientImpl(
      this.configuration,
      this.hooks,
      this.wsClient,
      this.actorSystem,
      this.transformRequest.andThen(transformRequest)
    )
}
