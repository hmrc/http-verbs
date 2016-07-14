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

import play.api.Play
import play.api.mvc.{Cookies, Headers, Session}
import uk.gov.hmrc.play.http.logging._

import scala.util.Try

case class UserId(value: String) extends AnyVal

case class Token(value: String) extends AnyVal

case class HeaderCarrier(authorization: Option[Authorization] = None,
                         userId: Option[UserId] = None,
                         token: Option[Token] = None,
                         forwarded: Option[ForwardedFor] = None,
                         sessionId: Option[SessionId] = None,
                         requestId: Option[RequestId] = None,
                         requestChain: RequestChain = RequestChain.init,
                         nsStamp: Long = System.nanoTime(),
                         extraHeaders: Seq[(String, String)] = Seq(),
                         trueClientIp: Option[String] = None,
                         trueClientPort: Option[String] = None,
                         gaToken: Option[String] = None,
                         gaUserId: Option[String] = None,
                         deviceID: Option[String] = None,
                         akamaiReputation: Option[AkamaiReputation] = None,
                         remainingHeaders: Seq[(String, String)] = Seq()) extends  LoggingDetails with HeaderProvider {

  /**
   * @return the time, in nanoseconds, since this header carrier was created
   */
  def age = System.nanoTime() - nsStamp

  val names = HeaderNames

  lazy val headers: Seq[(String, String)] = {
    List(requestId.map(rid => names.xRequestId -> rid.value),
      sessionId.map(sid => names.xSessionId -> sid.value),
      forwarded.map(f => names.xForwardedFor -> f.value),
      token.map(t => names.token -> t.value),
      Some(names.xRequestChain -> requestChain.value),
      authorization.map(auth => names.authorisation -> auth.value),
      trueClientIp.map(HeaderNames.trueClientIp ->_),
      trueClientPort.map(HeaderNames.trueClientPort ->_),
      gaToken.map(HeaderNames.googleAnalyticTokenId ->_),
      gaUserId.map(HeaderNames.googleAnalyticUserId ->_),
      deviceID.map(HeaderNames.deviceID -> _),
      akamaiReputation.map(HeaderNames.akamaiReputation -> _.value)
    ).flatten.toList ++ extraHeaders ++ remainingHeaders
  }

  def withExtraHeaders(headers:(String, String)*) : HeaderCarrier = {
    this.copy(extraHeaders = extraHeaders ++ headers)
  }
}

object HeaderCarrier {

  def fromHeadersAndSession(headers: Headers, session: Option[Session]=None) = {
    lazy val cookies: Cookies = Cookies(headers.get(play.api.http.HeaderNames.COOKIE))
    session.fold(fromHeaders(headers)) {
       fromSession(headers, cookies, _)
    }
  }

  private def getSessionId(s: Session, headers: Headers) = s.get(SessionKeys.sessionId).fold[Option[String]](headers.get(HeaderNames.xSessionId))(Some(_))

  private def getDeviceId(c: Cookies, headers: Headers) = c.get(CookieNames.deviceID).map(_.value).fold[Option[String]](headers.get(HeaderNames.deviceID))(Some(_))

  private def fromHeaders(headers: Headers): HeaderCarrier = {
    HeaderCarrier(
      headers.get(HeaderNames.authorisation).map(Authorization),
      None,
      headers.get(HeaderNames.token).map(Token),
      forwardedFor(headers),
      headers.get(HeaderNames.xSessionId).map(SessionId),
      headers.get(HeaderNames.xRequestId).map(RequestId),
      buildRequestChain(headers.get(HeaderNames.xRequestChain)),
      requestTimestamp(headers),
      Seq.empty,
      headers.get(HeaderNames.trueClientIp),
      headers.get(HeaderNames.trueClientPort),
      headers.get(HeaderNames.googleAnalyticTokenId),
      headers.get(HeaderNames.googleAnalyticUserId),
      headers.get(HeaderNames.deviceID),
      headers.get(HeaderNames.akamaiReputation).map(AkamaiReputation),
      remainingHeaders(headers)
    )
  }

  private def fromSession(headers: Headers, cookies: Cookies, s: Session): HeaderCarrier = {
    HeaderCarrier(
      s.get(SessionKeys.authToken).map(Authorization),
      s.get(SessionKeys.userId).map(UserId),
      s.get(SessionKeys.token).map(Token),
      forwardedFor(headers),
      getSessionId(s, headers).map(SessionId),
      headers.get(HeaderNames.xRequestId).map(RequestId),
      buildRequestChain(headers.get(HeaderNames.xRequestChain)),
      requestTimestamp(headers),
      Seq.empty,
      headers.get(HeaderNames.trueClientIp),
      headers.get(HeaderNames.trueClientPort),
      headers.get(HeaderNames.googleAnalyticTokenId),
      headers.get(HeaderNames.googleAnalyticUserId),
      getDeviceId(cookies, headers),
      headers.get(HeaderNames.akamaiReputation).map(AkamaiReputation),
      remainingHeaders(headers)
    )
  }

  def blacklistedHeaders(): Seq[String] = {
    // TODO sensible config key name and default values
    Play.current.configuration.getStringSeq("blacklistedHttpHeaders").getOrElse(Seq("User-Agent"))
  }


  private def remainingHeaders(headers: Headers): Seq[(String, String)] = {
    val remaining = headers.keys.
      filterNot(HeaderNames.explicitlyIncludedHeaders.keySet.contains(_)).
      filterNot(blacklistedHeaders().contains(_))
    remaining.map(h => (h -> headers.get(h).getOrElse(""))).toSeq
  }

  private def forwardedFor(headers: Headers): Option[ForwardedFor] = {
    ((headers.get(HeaderNames.trueClientIp), headers.get(HeaderNames.xForwardedFor)) match {
      case (tcip, None) => tcip
      case (None | Some(""), xff) => xff
      case (Some(tcip), Some(xff)) if xff.startsWith(tcip) => Some(xff)
      case (Some(tcip), Some(xff)) => Some(s"$tcip, $xff")
    }).map(ForwardedFor)
  }

  def buildRequestChain(currentChain: Option[String]): RequestChain = {
    currentChain match {
      case None => RequestChain.init
      case Some(chain) => RequestChain(chain).extend
    }
  }

  def requestTimestamp(headers: Headers): Long =
    headers
      .get(HeaderNames.xRequestTimestamp)
      .flatMap(tsAsString => Try(tsAsString.toLong).toOption)
      .getOrElse(System.nanoTime())




}
