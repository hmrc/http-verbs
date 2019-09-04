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

package uk.gov.hmrc.play.http.ws

import uk.gov.hmrc.http._

import scala.concurrent.Future

trait WSDelete extends CoreDelete with DeleteHttpTransport with WSRequest {

  override def doDelete(url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    buildRequest(url).withHeaders(headers: _*).delete().map(new WSHttpResponse(_))
  }

}
