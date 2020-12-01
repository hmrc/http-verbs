/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.ghik.silencer.silent
import play.api.Play
import play.api.mvc.{Cookies, Headers, RequestHeader, Session}
import uk.gov.hmrc.http._

import scala.util.Try

trait HeaderCarrierConverter {

  def fromRequest(request: RequestHeader) =
    fromHeaders(
      headers = request.headers,
      request = Some(request)
    )

  def fromRequestAndSession(request: RequestHeader, session: Session) =
    fromSession(
      headers = request.headers,
      cookies = Cookies.fromCookieHeader(request.headers.get(play.api.http.HeaderNames.COOKIE)),
      request = Some(request),
      session = session
    )

  @deprecated("Use fromRequest or fromRequestAndSession as appropiate", "13.0.0")
  def fromHeadersAndSession(headers: Headers, session: Option[Session] = None) =
    fromHeadersAndSessionAndRequest(headers, session, None)

  @deprecated("Use fromRequest or fromRequestAndSession as appropiate", "13.0.0")
  def fromHeadersAndSessionAndRequest(headers: Headers, session: Option[Session] = None, request: Option[RequestHeader] = None) = {
    lazy val cookies: Cookies = Cookies.fromCookieHeader(headers.get(play.api.http.HeaderNames.COOKIE))
    session.fold(fromHeaders(headers, request)) {
      fromSession(headers, cookies, request, _)
    }
  }

  def buildRequestChain(currentChain: Option[String]): RequestChain =
    currentChain
      .fold(RequestChain.init)(chain => RequestChain(chain).extend)

  def requestTimestamp(headers: Headers): Long =
    headers
      .get(HeaderNames.xRequestTimestamp)
      .flatMap(tsAsString => Try(tsAsString.toLong).toOption)
      .getOrElse(System.nanoTime())

  val Path = "path"

  private def getSessionId(session: Session, headers: Headers): Option[String] =
    session
      .get(SessionKeys.sessionId)
      .fold(headers.get(HeaderNames.xSessionId))(Some(_))

  private def getDeviceId(cookies: Cookies, headers: Headers): Option[String] =
    cookies
      .get(CookieNames.deviceID)
      .fold(headers.get(HeaderNames.deviceID))(cookie => Some(cookie.value))

  private def fromHeaders(headers: Headers, request: Option[RequestHeader]): HeaderCarrier =
    HeaderCarrier(
      authorization    = headers.get(HeaderNames.authorisation).map(Authorization),
      forwarded        = forwardedFor(headers),
      sessionId        = headers.get(HeaderNames.xSessionId).map(SessionId),
      requestId        = headers.get(HeaderNames.xRequestId).map(RequestId),
      requestChain     = buildRequestChain(headers.get(HeaderNames.xRequestChain)),
      nsStamp          = requestTimestamp(headers),
      extraHeaders     = Seq.empty,
      trueClientIp     = headers.get(HeaderNames.trueClientIp),
      trueClientPort   = headers.get(HeaderNames.trueClientPort),
      gaToken          = headers.get(HeaderNames.googleAnalyticTokenId),
      gaUserId         = headers.get(HeaderNames.googleAnalyticUserId),
      deviceID         = headers.get(HeaderNames.deviceID),
      akamaiReputation = headers.get(HeaderNames.akamaiReputation).map(AkamaiReputation),
      otherHeaders     = otherHeaders(headers, request)
    )

  private def fromSession(
    headers: Headers,
    cookies: Cookies,
    request: Option[RequestHeader],
    session: Session
  ): HeaderCarrier =
    HeaderCarrier(
      authorization    = session.get(SessionKeys.authToken).map(Authorization),
      forwarded        = forwardedFor(headers),
      sessionId        = getSessionId(session, headers).map(SessionId),
      requestId        = headers.get(HeaderNames.xRequestId).map(RequestId),
      requestChain     = buildRequestChain(headers.get(HeaderNames.xRequestChain)),
      nsStamp          = requestTimestamp(headers),
      extraHeaders     = Seq.empty,
      trueClientIp     = headers.get(HeaderNames.trueClientIp),
      trueClientPort   = headers.get(HeaderNames.trueClientPort),
      gaToken          = headers.get(HeaderNames.googleAnalyticTokenId),
      gaUserId         = headers.get(HeaderNames.googleAnalyticUserId),
      deviceID         = getDeviceId(cookies, headers),
      akamaiReputation = headers.get(HeaderNames.akamaiReputation).map(AkamaiReputation),
      otherHeaders     = otherHeaders(headers, request)
    )

  private def otherHeaders(headers: Headers, request: Option[RequestHeader]): Seq[(String, String)] =
    headers.headers
      .filterNot { case (k, _) => HeaderNames.explicitlyIncludedHeaders.map(_.toLowerCase).contains(k.toLowerCase) } ++
      // adding path so that play-auditing can access the request path without a dependency on play
      request.map(rh => Path -> rh.path).toSeq


  private def forwardedFor(headers: Headers): Option[ForwardedFor] =
    ((headers.get(HeaderNames.trueClientIp), headers.get(HeaderNames.xForwardedFor)) match {
      case (tcip, None)                                    => tcip
      case (None | Some(""), xff)                          => xff
      case (Some(tcip), Some(xff)) if xff.startsWith(tcip) => Some(xff)
      case (Some(tcip), Some(xff))                         => Some(s"$tcip, $xff")
    }).map(ForwardedFor)
}

object HeaderCarrierConverter extends HeaderCarrierConverter
