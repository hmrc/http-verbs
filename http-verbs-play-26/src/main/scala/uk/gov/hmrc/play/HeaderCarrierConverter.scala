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

package uk.gov.hmrc.play

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.mvc.{Cookies, Headers, RequestHeader, Session}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging._
import play.api.http.{HeaderNames => PlayHeaderNames}

import scala.util.Try

object HeaderCarrierConverter extends HeaderCarrierConverter {
  protected val configuration: Configuration = Configuration(ConfigFactory.load())
}

trait HeaderCarrierConverter {

  def fromHeadersAndSession(headers: Headers, session: Option[Session] = None): HeaderCarrier =
    fromHeadersAndSessionAndRequest(headers, session, None)

  def fromHeadersAndSessionAndRequest(
    headers: Headers,
    session: Option[Session]       = None,
    request: Option[RequestHeader] = None): HeaderCarrier =
    session.fold(fromHeaders(headers, request)) { session =>
      // Cookie setting changed between Play 2.5 and Play 2.6, this now checks both ways
      // cookie can be set for backwards compatibility
      val cookiesInHeader =
        Cookies.fromCookieHeader(headers.get(PlayHeaderNames.COOKIE)).toList
      val cookiesInSession =
        request.map(_.cookies).map(_.toList).getOrElse(List.empty)
      val cookies = Cookies(cookiesInSession ++ cookiesInHeader)
      fromSession(headers, cookies, request, session)
    }

  private def buildRequestChain(currentChain: Option[String]): RequestChain =
    currentChain match {
      case None        => RequestChain.init
      case Some(chain) => RequestChain(chain).extend
    }

  private def requestTimestamp(headers: Headers): Long =
    headers
      .get(HeaderNames.xRequestTimestamp)
      .flatMap(tsAsString => Try(tsAsString.toLong).toOption)
      .getOrElse(System.nanoTime())

  val Path = "path"

  private def getSessionId(s: Session, headers: Headers) =
    s.get(SessionKeys.sessionId).fold[Option[String]](headers.get(HeaderNames.xSessionId))(Some(_))

  private def getDeviceId(c: Cookies, headers: Headers) =
    c.get(CookieNames.deviceID).map(_.value).fold[Option[String]](headers.get(HeaderNames.deviceID))(Some(_))

  private def fromHeaders(headers: Headers, requestHeader: Option[RequestHeader]): HeaderCarrier =
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
      otherHeaders     = otherHeaders(headers, requestHeader)
    )

  private def fromSession(
    headers: Headers,
    cookies: Cookies,
    requestHeader: Option[RequestHeader],
    s: Session): HeaderCarrier =
    HeaderCarrier(
      authorization    = s.get(SessionKeys.authToken).map(Authorization),
      forwarded        = forwardedFor(headers),
      sessionId        = getSessionId(s, headers).map(SessionId),
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
      otherHeaders     = otherHeaders(headers, requestHeader)
    )

  private def otherHeaders(headers: Headers, requestHeader: Option[RequestHeader]): Seq[(String, String)] = {
    val remaining =
      headers.keys
        .filterNot(HeaderNames.explicitlyIncludedHeaders.contains(_))
        .filter(h => whitelistedHeaders.map(_.toLowerCase).contains(h.toLowerCase))
    remaining.map(h => h -> headers.get(h).getOrElse("")).toSeq ++
      //adding path so that play-auditing can access the request path without a dependency on play
      requestHeader.map(rh => Path -> rh.path)
  }

  private def whitelistedHeaders: Seq[String] =
    configuration.get[Seq[String]]("httpHeadersWhitelist")

  protected def configuration: Configuration

  private def forwardedFor(headers: Headers): Option[ForwardedFor] =
    ((headers.get(HeaderNames.trueClientIp), headers.get(HeaderNames.xForwardedFor)) match {
      case (tcip, None)                                    => tcip
      case (None | Some(""), xff)                          => xff
      case (Some(tcip), Some(xff)) if xff.startsWith(tcip) => Some(xff)
      case (Some(tcip), Some(xff))                         => Some(s"$tcip, $xff")
    }).map(ForwardedFor)

}
