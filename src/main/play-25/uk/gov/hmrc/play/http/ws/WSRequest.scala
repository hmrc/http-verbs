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

import play.api.libs.ws
import play.api.libs.ws.{DefaultWSProxyServer, WSClient, WSProxyServer}
import play.api.{Configuration, Play}
import uk.gov.hmrc.http.{HeaderCarrier, Request}

trait WSRequest extends Request {

  import play.api.libs.ws.WS

  def wsClient: WSClient = {
    import play.api.Play.current
    WS.client
  }

  def buildRequest[A](url: String)(implicit hc: HeaderCarrier): ws.WSRequest =
    wsClient.url(url).withHeaders(applicableHeaders(url)(hc): _*)

}

trait WSProxy extends WSRequest {

  def wsProxyServer: Option[WSProxyServer]

  override def buildRequest[A](url: String)(implicit hc: HeaderCarrier): ws.WSRequest =
    wsProxyServer match {
      case Some(proxy) => super.buildRequest(url).withProxyServer(proxy)
      case None        => super.buildRequest(url)
    }
}

object WSProxyConfiguration {

  def apply(configPrefix: String, configuration: Configuration): Option[WSProxyServer] = {
    val proxyRequired = configuration.getBoolean(s"$configPrefix.proxyRequiredForThisEnvironment").getOrElse(true)

    if (proxyRequired) Some(parseProxyConfiguration(configPrefix, configuration)) else None
  }

  def apply(configPrefix: String): Option[WSProxyServer] = {

    import play.api.Play.current
    apply(configPrefix, Play.configuration)

  }

  private def parseProxyConfiguration(configPrefix: String, configuration: Configuration) =
    DefaultWSProxyServer(
      protocol =
        configuration.getString(s"$configPrefix.protocol").orElse(throw ProxyConfigurationException("protocol")),
      host      = configuration.getString(s"$configPrefix.host").getOrElse(throw ProxyConfigurationException("host")),
      port      = configuration.getInt(s"$configPrefix.port").getOrElse(throw ProxyConfigurationException("port")),
      principal = configuration.getString(s"$configPrefix.username"),
      password  = configuration.getString(s"$configPrefix.password")
    )

  case class ProxyConfigurationException(key: String)
      extends RuntimeException(s"Missing proxy configuration - key '$key' not found")

}
