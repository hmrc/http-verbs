/*
 * Copyright 2023 HM Revenue & Customs
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

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.WSHttp

trait HttpClientSupport {
  def mkHttpClient(
    config: Config = ConfigFactory.load()
  ) =
    new HttpClient with WSHttp {
      private implicit val as: ActorSystem = ActorSystem("test-actor-system")

      override val wsClient: WSClient                 = AhcWSClient(AhcWSClientConfigFactory.forConfig(config))
      override protected val configuration: Config    = config
      override val hooks: Seq[HttpHook]               = Seq.empty
      override protected def actorSystem: ActorSystem = as
    }

  lazy val httpClient: HttpClient = mkHttpClient()
}
