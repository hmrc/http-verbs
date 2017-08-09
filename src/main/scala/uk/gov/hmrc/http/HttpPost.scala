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

package uk.gov.hmrc.http

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HttpVerbs.{POST => POST_VERB}
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpPost extends CorePost with PostHttpTransport with HttpVerb with ConnectionTracing with HttpHooks {

  override def POST[I, O](url: String, body: I, headers: Seq[(String, String)])(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] = {
    withTracing(POST_VERB, url) {
      val httpResponse = doPost(url, body, headers)
      executeHooks(url, POST_VERB, Option(Json.stringify(wts.writes(body))), httpResponse)
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }
  }

  override def POSTString[O](url: String, body: String, headers: Seq[(String, String)])(implicit rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] = {
    withTracing(POST_VERB, url) {
      val httpResponse = doPostString(url, body, headers)
      executeHooks(url, POST_VERB, Option(body), httpResponse)
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }
  }

  override def POSTForm[O](url: String, body: Map[String, Seq[String]])(implicit rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] = {
    withTracing(POST_VERB, url) {
      val httpResponse = doFormPost(url, body)
      executeHooks(url, POST_VERB, Option(body), httpResponse)
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }
  }

  override def POSTEmpty[O](url: String)(implicit rds: HttpReads[O], hc: HeaderCarrier, ec: ExecutionContext): Future[O] = {
    withTracing(POST_VERB, url) {
      val httpResponse = doEmptyPost(url)
      executeHooks(url, POST_VERB, None, httpResponse)
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }
  }

}
