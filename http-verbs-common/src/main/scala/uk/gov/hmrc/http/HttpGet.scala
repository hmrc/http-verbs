/*
 * Copyright 2022 HM Revenue & Customs
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

import java.net.URLEncoder

import uk.gov.hmrc.http.HttpVerbs.{GET => GET_VERB}
import uk.gov.hmrc.http.hooks.{HttpHooks, Payload, RequestData, ResponseData}
import uk.gov.hmrc.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpGet
  extends CoreGet
     with GetHttpTransport
     with HttpVerb
     with ConnectionTracing
     with HttpHooks
     with Retries {

  private lazy val hcConfig = HeaderCarrier.Config.fromConfig(configuration)

  override def GET[A](
    url        : String,
    queryParams: Seq[(String, String)],
    headers    : Seq[(String, String)]
  )(implicit
    rds: HttpReads[A],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] = {
    if (queryParams.nonEmpty && url.contains("?")) {
      throw new UrlValidationException(
        url,
        s"${this.getClass}.GET(url, queryParams)",
        "Query parameters should be provided in either url or as a Seq of tuples")
    }

    val urlWithQuery = url + makeQueryString(queryParams)

    withTracing(GET_VERB, urlWithQuery) {
      val allHeaders   = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(GET_VERB, urlWithQuery)(doGet(urlWithQuery, headers = allHeaders))
      executeHooks(
        GET_VERB,
        url"$url",
        RequestData(allHeaders, Payload(None)),
        httpResponse.map(ResponseData.fromHttpResponse)
      )
      mapErrors(GET_VERB, urlWithQuery, httpResponse).map(response => rds.read(GET_VERB, urlWithQuery, response))
    }
  }

  private def makeQueryString(queryParams: Seq[(String, String)]) = {
    val paramPairs = queryParams.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }
    if (paramPairs.isEmpty) "" else paramPairs.mkString("?", "&", "")
  }
}
