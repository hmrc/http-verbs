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

package uk.gov.hmrc.play.http.ws.default

import uk.gov.hmrc.http.{CoreDelete, DeleteHttpTransport, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.ws.{WSExecute, WSHttpResponse, WSRequestBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait WSDelete extends CoreDelete with DeleteHttpTransport with WSRequestBuilder with WSExecute {

  override def doDelete(
    url: String,
    headers: Seq[(String, String)])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse] =
    execute(buildRequest(url, headers), "DELETE")
      .map(WSHttpResponse.apply)
}
