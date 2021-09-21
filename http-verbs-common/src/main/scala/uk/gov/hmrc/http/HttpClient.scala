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

trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch {
  // TODO require implementations to not break clients e.g. implementations of ProxyHttpClient...
  def withUserAgent(userAgent: String): HttpClient =
    sys.error("Your implementation of HttpClient does not implement `withUserAgent`. Consider if you can use uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient") // TODO reference to bootstrap :(

  def withProxy(): HttpClient =
    sys.error("Your implementation of HttpClient does not implement `withProxy`. Consider if you can use uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient") // TODO reference to bootstrap :(
}
