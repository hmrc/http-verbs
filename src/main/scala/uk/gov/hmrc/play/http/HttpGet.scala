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

package uk.gov.hmrc.play.http

import java.net.URLEncoder

import play.api.http.HttpVerbs.{GET => GET_VERB}
import uk.gov.hmrc.play.http.hooks.HttpHooks
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.logging.{ConnectionTracing, MdcLoggingExecutionContext}

import scala.concurrent.Future

trait HttpGet extends HttpVerb with ConnectionTracing with HttpHooks {

  protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  def GET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] =withTracing(GET_VERB, url) {
    val httpResponse = doGet(url)
    executeHooks(url, GET_VERB, None, httpResponse)
    mapErrors(GET_VERB, url, httpResponse).map(response => rds.read(GET_VERB, url, response))
  }

  def GET[A](url: String, queryParams: Seq[(String, String)])(implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] = {
    val queryString = makeQueryString(queryParams)
    if (url.contains("?")) {
      throw new UrlValidationException(url, s"${this.getClass}.GET(url, queryParams)", "Query parameters must be provided as a Seq of tuples to this method")
    }
    GET(url + queryString)
  }

  private def makeQueryString(queryParams: Seq[(String,String)]) = {
    val paramPairs = queryParams.map(Function.tupled((k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}"))
    val params = paramPairs.mkString("&")

    if (params.isEmpty) "" else s"?$params"
  }
}
