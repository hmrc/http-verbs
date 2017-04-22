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

package uk.gov.hmrc.play.http.ws

import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import play.api.libs.ws.DefaultWSProxyServer
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.play.http.ws.WSProxyConfiguration.ProxyConfigurationException


class WSProxyConfigurationSpec extends WordSpecLike with Matchers with BeforeAndAfter {

  def proxyFlagConfiguredTo(value: Boolean): Map[String, Any] = Map("Dev.httpProxy.proxyRequiredForThisEnvironment" -> value)

  def proxyConfigWithFlagSetTo(flag: Option[Boolean] = None): Map[String, Any] = Map(
    "Dev.httpProxy.protocol" -> "https",
    "Dev.httpProxy.host" -> "localhost",
    "Dev.httpProxy.port" -> 7979,
    "Dev.httpProxy.username" -> "user",
    "Dev.httpProxy.password" -> "secret") ++ flag.fold(Map.empty[String, Any])(flag => proxyFlagConfiguredTo(flag))

  val proxy = DefaultWSProxyServer(
    protocol = Some("https"),
    host = "localhost",
    port = 7979,
    principal = Some("user"),
    password = Some("secret")
  )
  
  "If the proxyRequiredForThisEnvironment flag is not present, the WSProxyConfiguration apply method" should {

    "fail if no proxy is defined" in new WithApplication(FakeApplication()) {
      a [ProxyConfigurationException] should be thrownBy WSProxyConfiguration("Dev.httpProxy")
    }

    "return the proxy configuration if the proxy is defined" in new WithApplication(FakeApplication(additionalConfiguration = proxyConfigWithFlagSetTo(None))) {
      WSProxyConfiguration("Dev.httpProxy") shouldBe Some(proxy)
    }
  }

  "If the proxyRequiredForThisEnvironment flag is set to true, the WSProxyConfiguration apply method" should {

    "fail if no proxy is defined" in new WithApplication(FakeApplication(additionalConfiguration = proxyFlagConfiguredTo(value = true))) {
      a [ProxyConfigurationException] should be thrownBy WSProxyConfiguration("Dev.httpProxy")
    }

    "return the proxy configuration if the proxy is defined" in new WithApplication(FakeApplication(additionalConfiguration = proxyConfigWithFlagSetTo(Some(true)))) {
      WSProxyConfiguration("Dev.httpProxy") shouldBe Some(proxy)
    }
  }

  "If the proxyRequiredForThisEnvironment flag is set to false, the WSProxyConfiguration apply method" should {
    "return None if no proxy is defined" in new WithApplication(FakeApplication(additionalConfiguration = proxyFlagConfiguredTo(value = false))) {
      WSProxyConfiguration("Dev.httpProxy") shouldBe None
    }

    "return None if the proxy is defined" in new WithApplication(FakeApplication(additionalConfiguration = proxyConfigWithFlagSetTo(Some(false)))) {
      WSProxyConfiguration("Dev.httpProxy") shouldBe None
    }
  }
}
