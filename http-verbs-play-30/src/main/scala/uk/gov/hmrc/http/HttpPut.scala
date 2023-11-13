/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.http.HttpVerbs.{PUT => PUT_VERB}
import uk.gov.hmrc.http.hooks.{Data, HookData, HttpHooks, RequestData, ResponseData}
import uk.gov.hmrc.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpPut
  extends CorePut
     with PutHttpTransport
     with HttpVerb
     with ConnectionTracing
     with HttpHooks
     with Retries {

  private lazy val hcConfig = HeaderCarrier.Config.fromConfig(configuration)

  override def PUT[I, O](
    url    : String,
    body   : I,
    headers: Seq[(String, String)]
  )(implicit
    wts: Writes[I],
    rds: HttpReads[O],
    hc : HeaderCarrier,
    ec : ExecutionContext
  ): Future[O] =
    withTracing(PUT_VERB, url) {
      val allHeaders = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(PUT_VERB, url)(doPut(url, body, allHeaders))
      executeHooks(
        PUT_VERB,
        url"$url",
        RequestData(allHeaders, Some(Data.pure(HookData.FromString(Json.stringify(wts.writes(body)))))),
        httpResponse.map(ResponseData.fromHttpResponse)
      )
      mapErrors(PUT_VERB, url, httpResponse).map(response => rds.read(PUT_VERB, url, response))
    }

  override def PUTString[O](
    url    : String,
    body   : String,
    headers: Seq[(String, String)]
  )(implicit
    rds: HttpReads[O],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[O] =
    withTracing(PUT_VERB, url) {
      val allHeaders = HeaderCarrier.headersForUrl(hcConfig, url, headers) :+ "Http-Client-Version" -> BuildInfo.version
      val httpResponse = retryOnSslEngineClosed(PUT_VERB, url)(doPutString(url, body, allHeaders))
      executeHooks(
        PUT_VERB,
        url"$url",
        RequestData(allHeaders, Some(Data.pure(HookData.FromString(body)))),
        httpResponse.map(ResponseData.fromHttpResponse)
      )
      mapErrors(PUT_VERB, url, httpResponse).map(rds.read(PUT_VERB, url, _))
    }
}
