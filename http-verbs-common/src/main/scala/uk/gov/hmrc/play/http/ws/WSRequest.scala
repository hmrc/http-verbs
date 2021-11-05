/*
 * Copyright 2022 HM Revenue & Customs
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

import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSProxyServer, WSRequest => PlayWSRequest}

trait WSRequest extends WSRequestBuilder {

  override def buildRequest(
    url    : String,
    headers: Seq[(String, String)]
  ): PlayWSRequest =
    wsClient.url(url)
      .withHttpHeaders(headers: _*)
}

trait WSProxy extends WSRequest {

  def wsProxyServer: Option[WSProxyServer]

  override def buildRequest(url: String, headers: Seq[(String, String)]): PlayWSRequest =
    wsProxyServer match {
      case Some(proxy) => super.buildRequest(url, headers).withProxyServer(proxy)
      case None        => super.buildRequest(url, headers)
    }
}

object WSProxyConfiguration {

  @deprecated("Use buildWsProxyServer instead.", "14.0.0") // TODO document differences
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


  def buildWsProxyServer(configuration: Config): Option[WSProxyServer] = {
    def getOptionalString(key: String): Option[String] =
      if (configuration.hasPath(key)) Some(configuration.getString(key)) else None

    if (configuration.getBoolean("proxy.enabled"))
      Some(
        DefaultWSProxyServer(
          protocol  = Some(configuration.getString("proxy.protocol")),
          host      = configuration.getString("proxy.host"),
          port      = configuration.getInt("proxy.port"),
          principal = getOptionalString("proxy.username"),
          password  = getOptionalString("proxy.password")
        )
      )
    else None
  }
}
