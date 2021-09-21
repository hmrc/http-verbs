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

package uk.gov.hmrc.http.test

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.github.ghik.silencer.silent
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.WSHttp
import play.api.libs.ws.ahc.{AhcWSClientConfig, AhcWSClientConfigFactory}
import play.api.libs.ws.WSProxyServer

trait HttpClientSupport {
  def mkHttpClient(
    config      : Config            = ConfigFactory.load(),
    ahcWsCconfig: AhcWSClientConfig = AhcWSClientConfig()
  ) =
    new StandaloneHttpClient(config, ahcWsCconfig, wsProxyServer = None)

  lazy val httpClient: HttpClient = mkHttpClient()
}

private[test] class StandaloneHttpClient(
  config                    : Config,
  ahcWsConfig               : AhcWSClientConfig,
  override val wsProxyServer: Option[WSProxyServer]
) extends HttpClient with WSHttp {

  private implicit val as: ActorSystem = ActorSystem("test-actor-system")

  @silent("deprecated")
  private implicit val mat: Materializer = ActorMaterializer() // explicitly required for play-26

  override def wsClient: WSClient                 = play.api.libs.ws.ahc.AhcWSClient(AhcWSClientConfigFactory.forConfig(config))
  override protected def configuration: Config    = config
  override val hooks: Seq[HttpHook]               = Seq.empty
  override protected def actorSystem: ActorSystem = as

  override def withUserAgent(userAgent: String): HttpClient = {
    import scala.collection.JavaConverters._
    new StandaloneHttpClient(
      config = ConfigFactory.parseMap(Map("appName" -> userAgent).asJava).withFallback(config),
      ahcWsConfig,
      wsProxyServer
    )
  }

  override def withProxy(): HttpClient =
    this
    // TODO is there any reason why we'd not want to silently ignore the proxy for tests?
    /*new StandaloneHttpClient(
      config,
      ahcWsConfig,
      wsProxyServer = Some(
          DefaultWSProxyServer(
            protocol  = Some(config.get[String]("proxy.protocol")),
            host      = config.get[String]("proxy.host"),
            port      = config.get[Int]("proxy.port"),
            principal = config.getOptional[String]("proxy.username"),
            password  = config.getOptional[String]("proxy.password")
          )
        )
    )*/
}
