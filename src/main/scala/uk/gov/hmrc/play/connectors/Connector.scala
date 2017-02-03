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

package uk.gov.hmrc.play.connectors

import play.api.Play.current
import play.api.libs.ws.{WS, WSRequest}
import uk.gov.hmrc.play.http.HeaderCarrier

trait RequestBuilder {
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest
}

trait PlayWSRequestBuilder extends RequestBuilder {
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest = WS.url(url).withHeaders(hc.headers: _*)
}

trait WSClientRequestBuilder extends RequestBuilder {
  this: WSClientProvider =>
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest = client.url(url).withHeaders(hc.headers: _*)
}

@deprecated("Please use PlayWSRequestBuilder instead", "3.1.0")
trait Connector extends PlayWSRequestBuilder
