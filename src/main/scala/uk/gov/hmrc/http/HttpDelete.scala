/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.http

import uk.gov.hmrc.http.HttpVerbs.{DELETE => DELETE_VERB}
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpDelete
    extends CoreDelete
    with DeleteHttpTransport
    with HttpVerb
    with ConnectionTracing
    with HttpHooks
    with Retries {

  override def DELETE[O](url: String, headers: Seq[(String, String)] = Seq.empty)(implicit rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] =
    withTracing(DELETE_VERB, url) {
      val httpResponse = retry(DELETE_VERB, url)(doDelete(url, headers))
      executeHooks(url, DELETE_VERB, None, httpResponse)
      mapErrors(DELETE_VERB, url, httpResponse).map(rds.read(DELETE_VERB, url, _))
    }
}
