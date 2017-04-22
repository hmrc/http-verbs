/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import MdcLoggingExecutionContext._

import scala.concurrent.Future

trait WSGet extends HttpGet with WSRequest {

  def doGet(url: String)(implicit  hc: HeaderCarrier): Future[HttpResponse] = {
    buildRequest(url).get().map(new WSHttpResponse(_))
  }
}
