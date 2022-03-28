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

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HttpVerbs.{POST => POST_VERB}
import uk.gov.hmrc.http.hooks.{HookData, HttpHooks, ResponseData}
import uk.gov.hmrc.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpPost
  extends CorePost
     with PostHttpTransport
     with HttpVerb
     with ConnectionTracing
     with HttpHooks
     with Retries {

  private lazy val hcConfig = HeaderCarrier.Config.fromConfig(configuration)

  override def POST[I, O](
    url    : String,
    body   : I,
    headers: Seq[(String, String)]
  )(implicit
    wts: Writes[I],
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    withTracing(POST_VERB, url) {
      val allHeaders = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(POST_VERB, url)(doPost(url, body, allHeaders))
      executeHooks(
        POST_VERB,
        url"$url",
        allHeaders,
        Option(HookData.FromString(Json.stringify(wts.writes(body)), isTruncated = false)),
        httpResponse.map(ResponseData(_, isTruncated = false))
      )
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }

  override def POSTString[O](
    url    : String,
    body   : String,
    headers: Seq[(String, String)]
  )(implicit
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    withTracing(POST_VERB, url) {
      val allHeaders = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(POST_VERB, url)(doPostString(url, body, allHeaders))
      executeHooks(
        POST_VERB,
        url"$url",
        allHeaders,
        Option(HookData.FromString(body, isTruncated = false)),
        httpResponse.map(ResponseData(_, isTruncated = false))
      )
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }

  override def POSTForm[O](
    url    : String,
    body   : Map[String, Seq[String]],
    headers: Seq[(String, String)]
  )(implicit
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    withTracing(POST_VERB, url) {
      val allHeaders = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(POST_VERB, url)(doFormPost(url, body, allHeaders))
      executeHooks(
        POST_VERB,
        url"$url",
        allHeaders,
        Option(HookData.FromMap(body)),
        httpResponse.map(ResponseData(_, isTruncated = true))
      )
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }

  override def POSTEmpty[O](
    url    : String,
    headers: Seq[(String, String)]
  )(implicit
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    withTracing(POST_VERB, url) {
      val allHeaders = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(POST_VERB, url)(doEmptyPost(url, allHeaders))
      executeHooks(
        POST_VERB,
        url"$url",
        allHeaders,
        None,
        httpResponse.map(ResponseData(_, isTruncated = false))
      )
      mapErrors(POST_VERB, url, httpResponse).map(rds.read(POST_VERB, url, _))
    }
}
