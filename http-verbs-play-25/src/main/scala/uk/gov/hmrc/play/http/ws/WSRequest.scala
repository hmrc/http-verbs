/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.ghik.silencer.silent
import play.api.libs.ws.{DefaultWSProxyServer, WSClient, WSProxyServer, WSRequest => PlayWSRequest }
import play.api.{Configuration, Play, Logger}
import uk.gov.hmrc.http.HeaderCarrier

trait WSRequest extends WSRequestBuilder {

  private val logger = Logger(getClass)

  import play.api.libs.ws.WS

  @silent("deprecated")
  override def wsClient: WSClient =
    WS.client(play.api.Play.current)

  override def buildRequest[A](
    url    : String,
    headers: Seq[(String, String)] = Seq.empty
  )(implicit
    hc: HeaderCarrier
  ): PlayWSRequest = {
    val hdrs = hc.headersForUrl(configuration)(url) ++ headers

    val duplicates = hdrs.groupBy(_._1).filter(_._2.length > 1).map(_._1)
    if (duplicates.nonEmpty)
      logger.warn(s"The following headers were detected multiple times: ${duplicates.mkString(",")}")

    wsClient.url(url)
      .withHeaders(hdrs: _*)
  }
}

trait WSProxy extends WSRequest {

  def wsProxyServer: Option[WSProxyServer]

  override def buildRequest[A](
    url    : String,
    headers: Seq[(String, String)]
  )(implicit
    hc: HeaderCarrier
  ): PlayWSRequest =
    wsProxyServer match {
      case Some(proxy) => super.buildRequest(url, headers).withProxyServer(proxy)
      case None        => super.buildRequest(url, headers)
    }
}

object WSProxyConfiguration {

  def apply(configPrefix: String, configuration: Configuration): Option[WSProxyServer] = {
    val proxyRequired = configuration.getBoolean(s"$configPrefix.proxyRequiredForThisEnvironment").getOrElse(true)

    if (proxyRequired) Some(parseProxyConfiguration(configPrefix, configuration)) else None
  }

  @silent("deprecated")
  def apply(configPrefix: String): Option[WSProxyServer] =
    apply(configPrefix, play.api.Play.current.configuration)

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
