/*
 * Copyright 2015 HM Revenue & Customs
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

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{Precondition, HeaderCarrier, HttpPut, HttpResponse}

import scala.concurrent.Future

trait WSPut extends HttpPut with WSRequest {

  def doPut[A](url: String, body: A, precondition: Precondition)
              (implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url, precondition).put(Json.toJson(body)).map(new WSHttpResponse(_))
  }
}
