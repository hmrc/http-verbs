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

package uk.gov.hmrc.play.http

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.libs.ws.{WSClient, WSRequest => PlayWSRequest}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

// class is final, since any overrides would be lost in the result of `withPlayWSRequest`
final class HttpClientImpl (
  override val configuration   : Config,
  override val hooks           : Seq[HttpHook],
  override val wsClient        : WSClient,
  override val actorSystem     : ActorSystem,
  override val transformRequest: PlayWSRequest => PlayWSRequest
) extends HttpClient
     with WSHttp {

  override def withTransformRequest(transformRequest: PlayWSRequest => PlayWSRequest): HttpClientImpl =
    new HttpClientImpl(
      this.configuration,
      this.hooks,
      this.wsClient,
      this.actorSystem,
      this.transformRequest.andThen(transformRequest)
    )
}
