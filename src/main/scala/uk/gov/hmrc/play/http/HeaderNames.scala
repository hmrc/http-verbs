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

package uk.gov.hmrc.play.http

object HeaderNames {

  import play.api.http.HeaderNames.AUTHORIZATION

  val authorisation = AUTHORIZATION
  val xForwardedFor = "x-forwarded-for"
  val xRequestId = "X-Request-ID"
  val xRequestTimestamp = "X-Request-Timestamp"
  val xSessionId = "X-Session-ID"
  val xRequestChain = "X-Request-Chain"
  val trueClientIp = "True-Client-IP"
  val trueClientPort = "True-Client-Port"
  val token = "token"
  val surrogate = "Surrogate"
  val otacAuthorization = "Otac-Authorization"
}

object SessionKeys {
  val sessionId = "sessionId"
  val userId = "userId"
  val name = "name"
  val email = "email"
  val agentName = "agentName"
  val token = "token"
  val authToken = "authToken"
  val otacToken = "otacToken"
  val affinityGroup = "affinityGroup"
  val authProvider = "ap"
  val lastRequestTimestamp = "ts"
  val redirect = "login_redirect"
  val npsVersion = "nps-version"
  val sensitiveUserId = "suppressUserIs"
  val postLogoutPage = "postLogoutPage"
  val loginOrigin = "loginOrigin"
  val portalRedirectUrl = "portalRedirectUrl"
  val portalState = "portalState"
}
