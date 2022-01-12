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

package uk.gov.hmrc.play.http.test

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

trait ResponseMatchers extends ScalaFutures with IntegrationPatience {

  /**
   * Enables syntax like:
   * <code>resource("/write/audit").post(validAuditRequest) should <b>have (status (204))</b></code>
   */
  def status(expected: Int) = new HavePropertyMatcher[Future[WSResponse], Int] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.status == expected,
      propertyName = "Response HTTP Status",
      expectedValue = expected,
      actualValue = response.futureValue.status
    )
  }

  /**
   * Enables syntax like:
   * <code>resource("/write/audit").post(validAuditRequest) should <b>have (body ("Invalid nino: !@£$%^&*^"))</b></code>
   */
  def body(expected: String) = new HavePropertyMatcher[Future[WSResponse], String] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.body == expected,
      propertyName = "Response Body",
      expectedValue = expected,
      actualValue = response.futureValue.body
    )
  }

  /**
   * Enables syntax like:
   * <code>resource("/write/audit").post(validAuditRequest) should <b>have (jsonContent ("""{ "valid": true }"""))</b></code>
   */
  def jsonContent(expected: String) = new HavePropertyMatcher[Future[WSResponse], JsValue] {
    val expectedAsJson = Json.parse(expected)

    def apply(response: Future[WSResponse]) = {
      HavePropertyMatchResult(
        matches = response.futureValue.json == expectedAsJson,
        propertyName = "Response Content JSON",
        expectedValue = expectedAsJson,
        actualValue = response.futureValue.json
      )
    }
  }

  /**
   * Checks if a property is defined and has a specific value
   * Enables syntax like:
   * <code>resource("/write/audit").post(validAuditRequest) should <b>have (jsonProperty (__ \ "valid", true))</b></code>
   */
  def jsonProperty[E](path: JsPath, expected: E)(implicit eReads: Reads[E]) = new HavePropertyMatcher[Future[WSResponse], String] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.json.validate(path.read[E]).map(_ == expected).getOrElse(false),
      propertyName = "Response JSON at path " + path,
      expectedValue = expected.toString,
      actualValue = {
        val json = response.futureValue.json
        json.validate(path.read[E]).map(_.toString).getOrElse(json.toString)
      }
    )
  }

  /**
   * Checks if a property is defined
   * Enables syntax like:
   * <code>resource("/write/audit").post(validAuditRequest) should <b>have (jsonProperty (__ \ "valid"))</b></code>
   */
  def jsonProperty(path: JsPath) = new HavePropertyMatcher[Future[WSResponse], JsValue] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.json.validate(path.readNullable[JsValue]).get.isDefined,
      propertyName = "Response JSON at path " + path,
      expectedValue = JsString("defined"),
      actualValue = response.futureValue.json.validate(path.readNullable[JsValue]).get.getOrElse(JsNull)
    )
  }
}

object ResponseMatchers extends ResponseMatchers
