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

package uk.gov.hmrc.http

import uk.gov.hmrc.http.logging._

case class HeaderCarrier(
  authorization   : Option[Authorization]    = None,
  forwarded       : Option[ForwardedFor]     = None,
  sessionId       : Option[SessionId]        = None,
  requestId       : Option[RequestId]        = None,
  requestChain    : RequestChain             = RequestChain.init,
  nsStamp         : Long                     = System.nanoTime(),
  extraHeaders    : Seq[(String, String)]    = Seq(),
  trueClientIp    : Option[String]           = None,
  trueClientPort  : Option[String]           = None,
  gaToken         : Option[String]           = None,
  gaUserId        : Option[String]           = None,
  deviceID        : Option[String]           = None,
  akamaiReputation: Option[AkamaiReputation] = None,
  otherHeaders    : Seq[(String, String)]    = Seq()
)
    extends LoggingDetails
    with HeaderProvider {

  /**
    * @return the time, in nanoseconds, since this header carrier was created
    */
  def age = System.nanoTime() - nsStamp

  val names = HeaderNames

  lazy val headers: Seq[(String, String)] = {
    List(
      requestId.map(rid => names.xRequestId  -> rid.value),
      sessionId.map(sid => names.xSessionId  -> sid.value),
      forwarded.map(f => names.xForwardedFor -> f.value),
      Some(names.xRequestChain -> requestChain.value),
      authorization.map(auth => names.authorisation -> auth.value),
      trueClientIp.map(HeaderNames.trueClientIp         -> _),
      trueClientPort.map(HeaderNames.trueClientPort     -> _),
      gaToken.map(HeaderNames.googleAnalyticTokenId     -> _),
      gaUserId.map(HeaderNames.googleAnalyticUserId     -> _),
      deviceID.map(HeaderNames.deviceID                 -> _),
      akamaiReputation.map(HeaderNames.akamaiReputation -> _.value)
    ).flatten ++
      extraHeaders ++
      otherHeaders
  }

  def withExtraHeaders(headers: (String, String)*): HeaderCarrier =
    this.copy(extraHeaders = extraHeaders ++ headers)

  def headersForUrl(config: Option[com.typesafe.config.Config])(url: String): Seq[(String, String)] = {
    import java.net.URL
    import scala.collection.JavaConverters.iterableAsScalaIterableConverter
    import scala.util.matching.Regex

    val internalHostPatterns: Seq[Regex] =
      config match {
        case Some(config) if config.hasPathOrNull("internalServiceHostPatterns") =>
          config.getStringList("internalServiceHostPatterns").asScala.map(_.r).toSeq
        case _ =>
          Seq("^.*\\.service$".r, "^.*\\.mdtp$".r)
      }

    val userAgentHeader: Seq[(String, String)] =
      config match {
        case Some(config) if config.hasPathOrNull("appName") =>
          Seq("User-Agent" -> config.getString("appName"))
        case _ =>
          Seq.empty
      }

    val hdrs = if (internalHostPatterns.exists(_.pattern.matcher(new URL(url).getHost).matches())) {
      headers
    } else {
      headers.filterNot(otherHeaders.contains(_))
    }

    hdrs ++ userAgentHeader
  }
}
