/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.ws.{WSRequest => PlayWSRequest}

trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch {
  // implementations provided to not break clients (which won't be using the new functions)
  // e.g. implementations of ProxyHttpClient, and the deprecated DefaultHttpClient in bootstrap.
  def withUserAgent(userAgent: String): HttpClient =
    sys.error("Not implemented by your implementation of HttpClient. You can use uk.gov.hmrc.http.PlayHttpClient")

  def withProxy: HttpClient =
    sys.error("Not implemented by your implementation of HttpClient. You can use uk.gov.hmrc.http.PlayHttpClient")

  def withTransformRequest(transform: PlayWSRequest => PlayWSRequest): HttpClient =
    sys.error("Your implementation of HttpClient does not implement `withTransformRequest`. You can use uk.gov.hmrc.http.PlayHttpClient")
}
