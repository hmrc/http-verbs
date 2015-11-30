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

import play.api.http.HttpVerbs.{PATCH => PATCH_VERB}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.http.hooks.HttpHooks
import uk.gov.hmrc.play.http.logging.ConnectionTracing
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait HttpPatch extends HttpVerb with ConnectionTracing with HttpHooks {
  protected def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse]

  def PATCH[I, O](url: String, body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier): Future[O] = {
    withTracing(PATCH_VERB, url) {
      val httpResponse = doPatch(url, body)
      executeHooks(url, PATCH_VERB, Option(Json.stringify(wts.writes(body))), httpResponse)
      mapErrors(PATCH_VERB, url, httpResponse).map(response => rds.read(PATCH_VERB, url, response))
    }
  }
}
