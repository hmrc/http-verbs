/*
 * Copyright 2015 HM Revenue & Customs
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

import play.api.Play
import play.api.libs.ws.{DefaultWSProxyServer, WSProxyServer}
import uk.gov.hmrc.play.http.HeaderCarrier

trait WSRequest {

  import play.api.Play.current
  import play.api.libs.ws.WS
  import uk.gov.hmrc.play.http.HeaderCarrier

  def buildRequest[A](url: String)(implicit hc: HeaderCarrier) = {
    WS.url(url).withHeaders(hc.headers: _*)
  }
}


trait WSProxy extends WSRequest {

  def wsProxyServer: Option[WSProxyServer]

  override def buildRequest[A](url: String)(implicit hc: HeaderCarrier) = {
    wsProxyServer match {
      case Some(proxy) => super.buildRequest(url).withProxyServer(proxy)
      case None => super.buildRequest(url)
    }
  }
}

object WSProxyConfiguration {

  import play.api.Play.current

  def apply(configPrefix: String): Option[WSProxyServer] = {

    val proxyRequired = Play.configuration.getBoolean(s"$configPrefix.proxyRequiredForThisEnvironment").getOrElse(true)

    if (proxyRequired) Some(parseProxyConfiguration(configPrefix)) else None
  }

  private def parseProxyConfiguration(configPrefix: String) = {
    DefaultWSProxyServer(
      protocol = Play.configuration.getString(s"$configPrefix.protocol").orElse(throw ProxyConfigurationException("protocol")),
      host = Play.configuration.getString(s"$configPrefix.host").getOrElse(throw ProxyConfigurationException("host")),
      port = Play.configuration.getInt(s"$configPrefix.port").getOrElse(throw ProxyConfigurationException("port")),
      principal = Play.configuration.getString(s"$configPrefix.username"),
      password = Play.configuration.getString(s"$configPrefix.password")
    )
  }

  case class ProxyConfigurationException(key: String) extends RuntimeException(s"Missing proxy configuration - key '$key' not found")

}
