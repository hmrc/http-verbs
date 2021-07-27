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

package uk.gov.hmrc.play.http.ws

import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSProxyServer, WSRequest => PlayWSRequest}

trait WSRequest extends WSRequestBuilder {

  def wsProxyServer: Option[WSProxyServer] = None

  override def buildRequest[A](
    url    : String,
    headers: Seq[(String, String)]
  ): PlayWSRequest =
    wsProxyServer
      .foldLeft(wsClient.url(url).withHttpHeaders(headers: _*))(_ withProxyServer _)
}

@deprecated("WSProxy is not required. Behaviour has been inlined into WSRequest", "13.9.0")
trait WSProxy extends WSRequest {

  def wsProxyServer: Option[WSProxyServer]

  override def buildRequest[A](url: String, headers: Seq[(String, String)]): PlayWSRequest =
    wsProxyServer match {
      case Some(proxy) => super.buildRequest(url, headers).withProxyServer(proxy)
      case None        => super.buildRequest(url, headers)
    }
}

object WSProxyConfiguration {

  @deprecated("Use buildWsProxyServer instead. See docs for differences.", "13.9.0")
  def apply(configPrefix: String, configuration: Configuration): Option[WSProxyServer] = {
    val proxyRequired =
      configuration.getOptional[Boolean](s"$configPrefix.proxyRequiredForThisEnvironment").getOrElse(true)

    if (proxyRequired)
      Some(
        DefaultWSProxyServer(
          protocol  = Some(configuration.get[String](s"$configPrefix.protocol")),
          host      = configuration.get[String](s"$configPrefix.host"),
          port      = configuration.get[Int](s"$configPrefix.port"),
          principal = configuration.getOptional[String](s"$configPrefix.username"),
          password  = configuration.getOptional[String](s"$configPrefix.password")
        )
    )
    else None
  }

  def buildWsProxyServer(configuration: Configuration): Option[WSProxyServer] =
    if (configuration.get[Boolean]("proxy.enabled")) {
      import scala.collection.JavaConverters.iterableAsScalaIterableConverter
      Some(
        DefaultWSProxyServer(
          protocol      = Some(configuration.get[String]("proxy.protocol")),
          host          = configuration.get[String]("proxy.host"),
          port          = configuration.get[Int]("proxy.port"),
          principal     = configuration.getOptional[String]("proxy.username"),
          password      = configuration.getOptional[String]("proxy.password"),
          nonProxyHosts = Some(configuration.underlying.getStringList("proxy.nonProxyHosts").asScala.toSeq)
        )
      )
    }
    else None
}
