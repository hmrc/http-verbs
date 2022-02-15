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

package uk.gov.hmrc.http.test

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import uk.gov.hmrc.http.client.{HttpClientV2, HttpClientV2Impl}

trait HttpClientV2Support {

  def mkHttpClientV2(
    config: Configuration = Configuration(ConfigFactory.load())
  ): HttpClientV2 = {
    implicit val as: ActorSystem = ActorSystem("test-actor-system")

    new HttpClientV2Impl(
      wsClient = AhcWSClient(AhcWSClientConfigFactory.forConfig(config.underlying)),
      as,
      config,
      hooks    = Seq.empty,
    )
  }

  lazy val httpClientV2: HttpClientV2 = mkHttpClientV2()
}
