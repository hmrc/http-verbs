/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.play.http.test

import org.scalatest.{Matchers, Suite}
import play.api.libs.json.{JsNull, JsValue}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSRequest

trait HttpTestHelper extends ResponseMatchers {
  self: Suite with Matchers with WSRequest =>

  def verifyGETStatusCodeOnly(url: String, expectedStatus: Int, headers: Option[HeaderCarrier] = None)(implicit hc: HeaderCarrier = headers.getOrElse(HeaderCarrier())): Unit = {
    buildRequest(url).get() should have(status(expectedStatus))
  }

  def verifyPOSTStatusCodeOnly(url: String, expectedStatus: Int, body: JsValue = JsNull, headers: Option[HeaderCarrier] = None)(implicit hc: HeaderCarrier = headers.getOrElse(HeaderCarrier())): Unit = {
    buildRequest(url).post(body) should have(status(expectedStatus))
  }
}
