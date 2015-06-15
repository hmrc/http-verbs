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

package uk.gov.hmrc.play.audit.model

import org.scalatest.{Matchers, FlatSpec}
import play.api.mvc.Cookie
import play.api.test.{FakeRequest, FakeApplication, WithApplication}


class DeviceIdSpec extends FlatSpec with Matchers {

  val application = FakeApplication(
    additionalConfiguration = Map("logger.application" -> "OFF")
  )

  "device id" should "be extracted from the mdtpdi cookie" in new WithApplication(application) {
    val cookie = Cookie(name = "mdtpdi", value = "\"e09eebcc-9fd7-4b4d-abe7-88087a8e2741_7NiCRoHNO/3pJwKEWxRBQg==\"")
    val request = FakeRequest().withCookies(cookie)
    DeviceId(request) shouldBe Some(DeviceId("e09eebcc-9fd7-4b4d-abe7-88087a8e2741", "7NiCRoHNO/3pJwKEWxRBQg=="))
  }

  it should "be extracted from the mdtpdi cookie without extra double quotes" in new WithApplication(application) {
    val cookie = Cookie(name = "mdtpdi", value = "e09eebcc-9fd7-4b4d-abe7-88087a8e2741_7NiCRoHNO/3pJwKEWxRBQg==")
    val request = FakeRequest().withCookies(cookie)
    DeviceId(request) shouldBe Some(DeviceId("e09eebcc-9fd7-4b4d-abe7-88087a8e2741", "7NiCRoHNO/3pJwKEWxRBQg=="))
  }

  it should "should not be extracted if the cookie is missing" in new WithApplication(application) {
    DeviceId(FakeRequest()) shouldBe None
  }

  it should "should not be extracted if the cookie value is malformed" in new WithApplication(application) {
    def shouldBeNone(value: String) = {
      val cookie = Cookie(name = "mdtpdi", value = value)
      val request = FakeRequest().withCookies(cookie)
      DeviceId(request) shouldBe None
    }
    shouldBeNone("\"e09eebcc-9fd7-4b4d-abe7-88087a8e2741\"")
    shouldBeNone("\"e09eebcc-9fd7-4b4d-abe7-88087a8e2741_\"")
  }

}
