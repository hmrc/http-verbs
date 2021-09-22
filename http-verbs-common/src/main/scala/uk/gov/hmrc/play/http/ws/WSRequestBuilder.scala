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

import com.typesafe.config.Config
import play.api.libs.ws.{WSClient, WSRequest => PlayWSRequest}
import uk.gov.hmrc.http.Request


trait WSRequestBuilder extends Request {

  protected def configuration: Config

  protected def wsClient: WSClient

  // TODO move to package uk.gov.hmrc.ws, then can restore `protected`
  /*protected*/ def buildRequest(url: String, headers: Seq[(String, String)]): PlayWSRequest
}
