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

package uk.gov.hmrc.play.http.ws.default

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{CorePut, HttpResponse, PutHttpTransport}
import uk.gov.hmrc.play.http.ws.{WSExecute, WSHttpResponse, WSRequestBuilder}
import play.api.libs.ws.{writeableOf_JsValue, writeableOf_String}

import scala.concurrent.{ExecutionContext, Future}

trait WSPut extends CorePut with PutHttpTransport with WSRequestBuilder with WSExecute{

  override def doPut[A](
    url: String,
    body: A,
    headers: Seq[(String, String)]
  )(
      implicit rds: Writes[A],
      ec: ExecutionContext
  ): Future[HttpResponse] =
    execute(buildRequest(url, headers).withBody(Json.toJson(body)), "PUT")
      .map(WSHttpResponse.apply)

  override def doPutString(
    url: String,
    body: String,
    headers: Seq[(String, String)]
  )(
      implicit ec: ExecutionContext
  ): Future[HttpResponse] =
    execute(buildRequest(url, headers).withBody(body), "PUT")
      .map(WSHttpResponse.apply)
}
