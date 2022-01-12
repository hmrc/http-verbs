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

package uk.gov.hmrc.http

object HeaderNames {

  /*
   * this isn't ideal, but downstream apps still want to refer to typed header values
   * and guarantee their explicit allowlisting whilst "remaining headers" should avoid
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
  val authToken = "authToken"
  val otacToken = "otacToken"
  val lastRequestTimestamp = "ts"
  val redirect             = "login_redirect"
  val npsVersion           = "nps-version"
  val sensitiveUserId      = "suppressUserIs"
  val postLogoutPage       = "postLogoutPage"
  val loginOrigin          = "loginOrigin"
  val portalRedirectUrl    = "portalRedirectUrl"
  val portalState          = "portalState"
}

case class Authorization(value: String) extends AnyVal

case class SessionId(value: String) extends AnyVal

case class RequestId(value: String) extends AnyVal

case class AkamaiReputation(value: String) extends AnyVal

case class RequestChain(value: String) extends AnyVal {
  def extend = RequestChain(s"$value-${RequestChain.newComponent}")
}

object RequestChain {
  def newComponent = (scala.util.Random.nextInt() & 0xffff).toHexString

  def init = RequestChain(newComponent)
}

case class ForwardedFor(value: String) extends AnyVal
