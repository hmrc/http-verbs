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
  val token = "token"
  val surrogate = "Surrogate"
  val otacAuthorization = "Otac-Authorization"
}

object SessionKeys {
  val sessionId = "sessionId"
  val userId = "userId"
  val name = "name"
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
