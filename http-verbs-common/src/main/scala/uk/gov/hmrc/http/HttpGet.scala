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

package uk.gov.hmrc.http

import uk.gov.hmrc.http.HttpVerbs.{GET => GET_VERB}
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpGet extends CoreGet with GetHttpTransport with HttpVerb with ConnectionTracing with HttpHooks with Retries {

  override def GET[A](
    urlBuilder: UrlBuilder,
    headers: Seq[(String, String)])(implicit rds: HttpReads[A], hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

    val url = urlBuilder.toUrl.toString

    withTracing(GET_VERB, url) {
      val httpResponse = retry(GET_VERB, url)(doGet(url, headers = headers))
      executeHooks(url, GET_VERB, None, httpResponse)
      mapErrors(GET_VERB, url, httpResponse).map(response => rds.read(GET_VERB, url, response))
    }
  }
}
