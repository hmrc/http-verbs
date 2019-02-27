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

package uk.gov.hmrc.play.connectors

import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

trait RequestBuilder {
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest
}

object RequestBuilder {
  def headers(hc: HeaderCarrier): Seq[(String, String)] =
    hc.headers.filter {
      case (name, value) => name != HeaderCarrierConverter.Path
    }
}

trait WSClientRequestBuilder extends RequestBuilder {
  def client: WSClient
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest =
    client.url(url).withHttpHeaders(RequestBuilder.headers(hc): _*)
}
