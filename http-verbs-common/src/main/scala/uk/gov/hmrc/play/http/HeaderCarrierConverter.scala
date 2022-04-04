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

package uk.gov.hmrc.play.http

import play.api.http.{HeaderNames => PlayHeaderNames}
import play.api.mvc.{Cookies, Headers, RequestHeader, Session}
import uk.gov.hmrc.http._

import scala.util.Try

trait HeaderCarrierConverter {

  def fromRequest(request: RequestHeader): HeaderCarrier =
    buildHeaderCarrier(
      headers = request.headers,
      session = None,
      request = Some(request)
    )

  def fromRequestAndSession(request: RequestHeader, session: Session): HeaderCarrier =
    buildHeaderCarrier(
      headers = request.headers,
      session = Some(session),
      request = Some(request)
    )

  @deprecated("Use fromRequest or fromRequestAndSession as appropriate", "13.0.0")
  def fromHeadersAndSession(headers: Headers, session: Option[Session] = None): HeaderCarrier =
    buildHeaderCarrier(headers, session, request = None)

  @deprecated("Use fromRequest or fromRequestAndSession as appropriate", "13.0.0")
  def fromHeadersAndSessionAndRequest(
    headers: Headers,
    session: Option[Session]       = None,
    request: Option[RequestHeader] = None
  ): HeaderCarrier =
    buildHeaderCarrier(headers, session, request)

  private def buildRequestChain(currentChain: Option[String]): RequestChain =
    currentChain
      .fold(RequestChain.init)(chain => RequestChain(chain).extend)

  private def requestTimestamp(headers: Headers): Long =
    headers
      .get(HeaderNames.xRequestTimestamp)
      .flatMap(tsAsString => Try(tsAsString.toLong).toOption)
      .getOrElse(System.nanoTime())

  val Path = "path"

  private def lookupCookies(headers: Headers, request: Option[RequestHeader]): Cookies = {
    // Cookie setting changed between Play 2.5 and Play 2.6, this now checks both ways
    // cookie can be set for backwards compatibility
    val cookiesInHeader =
      Cookies.fromCookieHeader(headers.get(PlayHeaderNames.COOKIE)).toList
    val cookiesInSession =
      request.map(_.cookies).map(_.toList).getOrElse(List.empty)
    Cookies(cookiesInSession ++ cookiesInHeader)
  }

  private def buildHeaderCarrier(
    headers: Headers,
    session: Option[Session],
    request: Option[RequestHeader]
  ): HeaderCarrier = {
    lazy val cookies = lookupCookies(headers, request)
    HeaderCarrier(
      authorization    = // Note, if a session is provided, any Authorization header in the request will be ignored
                         session.fold(headers.get(HeaderNames.authorisation))(_.get(SessionKeys.authToken))
                           .map(Authorization),
      forwarded        = forwardedFor(headers),
      sessionId        = session.flatMap(_.get(SessionKeys.sessionId))
                           .orElse(headers.get(HeaderNames.xSessionId))
                           .map(SessionId),
      requestId        = headers.get(HeaderNames.xRequestId).map(RequestId),
      requestChain     = buildRequestChain(headers.get(HeaderNames.xRequestChain)),
      nsStamp          = requestTimestamp(headers),
      extraHeaders     = Seq.empty,
      trueClientIp     = headers.get(HeaderNames.trueClientIp),
      trueClientPort   = headers.get(HeaderNames.trueClientPort),
      gaToken          = headers.get(HeaderNames.googleAnalyticTokenId),
      gaUserId         = headers.get(HeaderNames.googleAnalyticUserId),
      deviceID         = session.flatMap(_ => cookies.get(CookieNames.deviceID).map(_.value))
                           .orElse(headers.get(HeaderNames.deviceID)),
      akamaiReputation = headers.get(HeaderNames.akamaiReputation).map(AkamaiReputation),
      otherHeaders     = otherHeaders(headers, request)
    )
  }

  private def otherHeaders(headers: Headers, request: Option[RequestHeader]): Seq[(String, String)] = {
    val explicitlyIncludedHeadersLc = HeaderNames.explicitlyIncludedHeaders.map(_.toLowerCase)
    headers.headers
      .filterNot { case (k, _) => explicitlyIncludedHeadersLc.contains(k.toLowerCase) } ++
      // adding path so that play-auditing can access the request path without a dependency on play
      request.map(rh => Path -> rh.path).toSeq
  }

  private def forwardedFor(headers: Headers): Option[ForwardedFor] =
    ((headers.get(HeaderNames.trueClientIp), headers.get(HeaderNames.xForwardedFor)) match {
      case (tcip, None)                                    => tcip
      case (None | Some(""), xff)                          => xff
      case (Some(tcip), Some(xff)) if xff.startsWith(tcip) => Some(xff)
      case (Some(tcip), Some(xff))                         => Some(s"$tcip, $xff")
    }).map(ForwardedFor)
}

object HeaderCarrierConverter extends HeaderCarrierConverter
