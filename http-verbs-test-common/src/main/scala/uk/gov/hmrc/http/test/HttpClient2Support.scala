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
import akka.stream.{ActorMaterializer, Materializer}
import com.github.ghik.silencer.silent
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import uk.gov.hmrc.http.play.{HttpClient2, HttpClient2Impl}

trait HttpClient2Support {

  def mkHttpClient2(
    config: Configuration = Configuration(ConfigFactory.load())
  ): HttpClient2 = {
    implicit val as: ActorSystem = ActorSystem("test-actor-system")

    @silent("deprecated")
    implicit val mat: Materializer = ActorMaterializer() // explicitly required for play-26

    new HttpClient2Impl(
      wsClient = AhcWSClient(AhcWSClientConfigFactory.forConfig(config.underlying)),
      as,
      config,
      hooks    = Seq.empty,
    )
  }

  lazy val httpClient2: HttpClient2 = mkHttpClient2()
}
