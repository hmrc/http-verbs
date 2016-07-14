/*
 * Copyright 2016 HM Revenue & Customs
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

  /*
   * this isn't ideal, but downstream apps still want to refer to typed header values
   * and guarantee their explicit whitelisting whilst "remaining headers" should avoid
   * duplicating these and creating unnecessary data on the wire.
   * We could just model as a list but then accessing known header names would
   * have to be done by magic number and would be susceptible to changes in ordering
   */
  val explicitlyIncludedHeaders = Map(
    AUTHORIZATION -> AUTHORIZATION,
    "x-forwarded-for" -> "x-forwarded-for",
    "X-Request-ID" -> "X-Request-ID",
    "X-Request-Timestamp" -> "X-Request-Timestamp",
    "X-Session-ID" -> "X-Session-ID",
    "X-Request-Chain" -> "X-Request-Chain",
    "True-Client-IP" -> "True-Client-IP",
    "True-Client-Port" -> "True-Client-Port",
    "token" -> "token",
    "Surrogate" -> "Surrogate",
    "Otac-Authorization" -> "Otac-Authorization",
    "ga-token" -> "ga-token",
    "ga-user-cookie-id" -> "ga-user-cookie-id",
    "deviceID" -> "deviceID", // not a typo, should be ID
    "Akamai-Reputation" -> "Akamai-Reputation"
  )

  val authorisation = explicitlyIncludedHeaders.get(AUTHORIZATION).get
  val xForwardedFor = explicitlyIncludedHeaders.get("x-forwarded-for").get
  val xRequestId = explicitlyIncludedHeaders.get("X-Request-ID").get
  val xRequestTimestamp = explicitlyIncludedHeaders.get("X-Request-Timestamp").get
  val xSessionId = explicitlyIncludedHeaders.get("X-Session-ID").get
  val xRequestChain = explicitlyIncludedHeaders.get("X-Request-Chain").get
  val trueClientIp = explicitlyIncludedHeaders.get("True-Client-IP").get
  val trueClientPort = explicitlyIncludedHeaders.get("True-Client-Port").get
  val token = explicitlyIncludedHeaders.get("token").get
  val surrogate = explicitlyIncludedHeaders.get("Surrogate").get
  val otacAuthorization = explicitlyIncludedHeaders.get("Otac-Authorization").get
  val googleAnalyticTokenId = explicitlyIncludedHeaders.get("ga-token").get
  val googleAnalyticUserId  = explicitlyIncludedHeaders.get("ga-user-cookie-id").get
  val deviceID  = explicitlyIncludedHeaders.get("deviceID").get
  val akamaiReputation = explicitlyIncludedHeaders.get("Akamai-Reputation").get
}

object CookieNames {
  val deviceID = "mdtpdi"
}

object SessionKeys {
  val sessionId = "sessionId"
  val userId = "userId"
  @deprecated("To be removed. Use internal services lookup instead","2016-06-24")
  val name = "name"
  @deprecated("To be removed. Use internal services lookup instead","2016-06-24")
  val email = "email"
  @deprecated("To be removed. Use internal services lookup instead","2016-06-24")
  val agentName = "agentName"
  @deprecated("Use internal services lookup instead","2016-06-24")
  val token = "token"
  val authToken = "authToken"
  val otacToken = "otacToken"
  @deprecated("Use internal services lookup instead","2016-06-24")
  val affinityGroup = "affinityGroup"
  @deprecated("Use internal services lookup instead","2016-06-24")
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
