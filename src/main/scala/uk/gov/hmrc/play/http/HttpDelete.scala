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

package uk.gov.hmrc.play.http

import play.api.http.HttpVerbs.{DELETE => DELETE_VERB}
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditing}
import uk.gov.hmrc.play.http.logging.{MdcLoggingExecutionContext, ConnectionTracing}
import uk.gov.hmrc.play.http.reads.HttpReads
import scala.concurrent.Future
import MdcLoggingExecutionContext._

trait HttpDelete extends HttpVerb with ConnectionTracing with HttpAuditing {

  protected def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  def DELETE[O](url: String)(implicit rds: HttpReads[O], hc: HeaderCarrier): Future[O] = withTracing(DELETE_VERB, url) {
    val httpResponse = doDelete(url)
    auditRequestWithResponseF(url, DELETE_VERB, None, httpResponse)
    mapErrors(DELETE_VERB, url, httpResponse).map(rds.read(DELETE_VERB, url, _))
  }
}
