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

import play.api.http.HttpVerbs.{GET => GET_VERB}
import uk.gov.hmrc.play.http.Precondition.NoPrecondition
import uk.gov.hmrc.play.http.hooks.HttpHooks
import uk.gov.hmrc.play.http.logging.ConnectionTracing
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait HttpGet extends HttpVerb with ConnectionTracing with HttpHooks {

  protected def doGet(url: String, precondition: Precondition)
                     (implicit hc: HeaderCarrier): Future[HttpResponse]

  def GET[A](url: String, precondition: Precondition = NoPrecondition)
            (implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] =
    withTracing(GET_VERB, url) {
      val httpResponse = doGet(url, precondition)
      executeHooks(url, GET_VERB, None, httpResponse)
      mapErrors(GET_VERB, url, httpResponse).map(response => rds.read(GET_VERB, url, response))
    }
}
