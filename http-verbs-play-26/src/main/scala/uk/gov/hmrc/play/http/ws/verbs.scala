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

import play.api.libs.ws.{EmptyBody, WSRequest => PlayWSRequest}

trait WSDelete extends default.WSDelete with WSRequest
trait WSGet    extends default.WSGet    with WSRequest
trait WSPatch  extends default.WSPatch  with WSRequest
trait WSPut    extends default.WSPut    with WSRequest
trait WSPost   extends default.WSPost   with WSRequest {
  override def withEmptyBody(request: PlayWSRequest): PlayWSRequest =
    request.withBody(EmptyBody).addHttpHeaders((play.api.http.HeaderNames.CONTENT_LENGTH -> "0"))
}

trait WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch
