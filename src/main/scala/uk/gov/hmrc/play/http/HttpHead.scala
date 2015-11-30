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

import play.api.http.HttpVerbs.{HEAD => HEAD_VERB}
import uk.gov.hmrc.play.http.hooks.HttpHooks
import uk.gov.hmrc.play.http.logging.ConnectionTracing
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait HttpHead extends HttpVerb with ConnectionTracing with HttpHooks {

  protected def doHead(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  def HEAD[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] =
    withTracing(HEAD_VERB, url) {
      val httpResponse = doHead(url)
      executeHooks(url, HEAD_VERB, None, httpResponse)
      mapErrors(HEAD_VERB, url, httpResponse).map(response => rds.read(HEAD_VERB, url, response))
    }
}
