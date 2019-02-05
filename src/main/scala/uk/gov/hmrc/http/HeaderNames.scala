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

package uk.gov.hmrc.http

object HeaderNames {

  /*
   * this isn't ideal, but downstream apps still want to refer to typed header values
   * and guarantee their explicit whitelisting whilst "remaining headers" should avoid
   * duplicating these and creating unnecessary data on the wire.
   * We could just model as a list but then accessing known header names would
   * have to be done by magic number and would be susceptible to changes in ordering
   */

  val authorisation         = "Authorization"
  val xForwardedFor         = "x-forwarded-for"
  val xRequestId            = "X-Request-ID"
  val xRequestTimestamp     = "X-Request-Timestamp"
  val xSessionId            = "X-Session-ID"
  val xRequestChain         = "X-Request-Chain"
  val trueClientIp          = "True-Client-IP"
  val trueClientPort        = "True-Client-Port"
  val token                 = "token"
  val surrogate             = "Surrogate"
  val otacAuthorization     = "Otac-Authorization"
  val googleAnalyticTokenId = "ga-token"
  val googleAnalyticUserId  = "ga-user-cookie-id"
  val deviceID              = "deviceID" // not a typo, should be ID
  val akamaiReputation      = "Akamai-Reputation"

  val explicitlyIncludedHeaders = Seq(
    authorisation,
    xForwardedFor,
    xRequestId,
    xRequestTimestamp,
    xSessionId,
    xRequestChain,
    trueClientIp,
    trueClientPort,
    token,
    surrogate,
    otacAuthorization,
    googleAnalyticTokenId,
    googleAnalyticUserId,
    deviceID, // not a typo, should be ID
    akamaiReputation
  )
}

object CookieNames {
  val deviceID = "mdtpdi"
}

object SessionKeys {
  val sessionId = "sessionId"
  val userId    = "userId"
  @deprecated("To be removed. Use internal services lookup instead", "2016-06-24")
  val name = "name"
  @deprecated("To be removed. Use internal services lookup instead", "2016-06-24")
  val email = "email"
  @deprecated("To be removed. Use internal services lookup instead", "2016-06-24")
  val agentName = "agentName"
  @deprecated("Use internal services lookup instead", "2016-06-24")
  val token     = "token"
  val authToken = "authToken"
  val otacToken = "otacToken"
  @deprecated("Use internal services lookup instead", "2016-06-24")
  val affinityGroup = "affinityGroup"
  @deprecated("Use internal services lookup instead", "2016-06-24")
  val authProvider         = "ap"
  val lastRequestTimestamp = "ts"
  val redirect             = "login_redirect"
  val npsVersion           = "nps-version"
  val sensitiveUserId      = "suppressUserIs"
  val postLogoutPage       = "postLogoutPage"
  val loginOrigin          = "loginOrigin"
  val portalRedirectUrl    = "portalRedirectUrl"
  val portalState          = "portalState"
}
