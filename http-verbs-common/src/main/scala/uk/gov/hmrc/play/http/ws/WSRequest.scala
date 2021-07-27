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

@deprecated("Trait has been inlined into WSRequest", "5.8.0")
trait WSProxy extends WSRequest {

  def wsProxyServer: Option[WSProxyServer]

  override def buildRequest[A](url: String, headers: Seq[(String, String)]): PlayWSRequest =
    wsProxyServer match {
      case Some(proxy) => super.buildRequest(url, headers).withProxyServer(proxy)
      case None        => super.buildRequest(url, headers)
    }
}

// TODO deprecate this
// - package should be uk.gov.hmrc.http...
// - It has an `apply` function which doesn't return an instance of `WSProxyConfiguration`
// - error messages hide the fully qualified name of the key
// - proxyRequiredForThisEnvironment should default to false (and have a better name)
// - `configPrefix` shouldn't be parameterised, then we can move defaults to reference.conf
// - `configPrefix` should be `bootstrap.http.proxy`?
// - Needs `nonProxyHosts` (can't reuse `internalServiceHostPatterns` since is regex format...)
// - Provide a ProxyWireMockSupport for testing? requires removing "localhost" from nonProxyHosts and running a mock?
// TODO @deprecated("Use ... instead", "5.8.0")
object WSProxyConfiguration {

  def apply(configPrefix: String, configuration: Configuration): Option[WSProxyServer] = {
    val proxyRequired =
      configuration.getOptional[Boolean](s"$configPrefix.proxyRequiredForThisEnvironment").getOrElse(true)

    if (proxyRequired) Some(parseProxyConfiguration(configPrefix, configuration)) else None
  }

  private def parseProxyConfiguration(configPrefix: String, configuration: Configuration) =
    DefaultWSProxyServer(
      protocol  = Some(configuration.get[String](s"$configPrefix.protocol")), // this defaults to https, do we need it to be required in configuration?
      host      = configuration.get[String](s"$configPrefix.host"),
      port      = configuration.get[Int](s"$configPrefix.port"),
      principal = configuration.getOptional[String](s"$configPrefix.username"),
      password  = configuration.getOptional[String](s"$configPrefix.password")/*,
      nonProxyHosts = Some(Seq("*.service", "*.mdtp", "localhost")) // TODO move to reference.conf*/
    )
}
