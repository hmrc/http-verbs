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

import java.util.concurrent.atomic.AtomicBoolean

import uk.gov.hmrc.http.logging._
import play.api.Logger // TODO this library shouldn't depend on play

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
) extends LoggingDetails
     with HeaderProvider {

  private val logger = Logger(getClass)

  /**
    * @return the time, in nanoseconds, since this header carrier was created
    */
  def age = System.nanoTime() - nsStamp

  private lazy val explicitHeaders: Seq[(String, String)] =
    Seq(
      HeaderNames.xRequestId            -> requestId.map(_.value),
      HeaderNames.xSessionId            -> sessionId.map(_.value),
      HeaderNames.xForwardedFor         -> forwarded.map(_.value),
      HeaderNames.xRequestChain         -> Some(requestChain.value),
      HeaderNames.authorisation         -> authorization.map(_.value),
      HeaderNames.trueClientIp          -> trueClientIp,
      HeaderNames.trueClientPort        -> trueClientPort,
      HeaderNames.googleAnalyticTokenId -> gaToken,
      HeaderNames.googleAnalyticUserId  -> gaUserId,
      HeaderNames.deviceID              -> deviceID,
      HeaderNames.akamaiReputation      -> akamaiReputation.map(_.value)
    ).collect { case (k, Some(v)) => (k, v) }

  def withExtraHeaders(headers: (String, String)*): HeaderCarrier =
    this.copy(extraHeaders = extraHeaders ++ headers)

  private val deprecationLogged = new AtomicBoolean(false)

  override def headersForUrl(config: Option[com.typesafe.config.Config])(url: String): Seq[(String, String)] = {
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

    val allowlistedHeaders: Seq[String] =
      config match {
        case Some(config) =>
          if (config.hasPath("httpHeadersWhitelist") && !deprecationLogged.getAndSet(true))
            logger.warn("Use of configuration key 'httpHeadersWhitelist' will be IGNORED. Use 'bootstrap.http.headersAllowlist' instead")
          config.getStringList("bootstrap.http.headersAllowlist").asScala.toSeq
        case None => Seq.empty
      }

    val userAgentHeader: Seq[(String, String)] =
      config match {
        case Some(config) if config.hasPathOrNull("appName") =>
          Seq("User-Agent" -> config.getString("appName"))
        case _ =>
          Seq.empty
      }

    val isInternalHost = internalHostPatterns.exists(_.pattern.matcher(new URL(url).getHost).matches())

    // TODO add allowList to ensure we're not sending all explicit headers to external (e.g. Authorization)
    if (isInternalHost)
      explicitHeaders ++
        extraHeaders ++
        otherHeaders.filter { case (k, _) => allowlistedHeaders.map(_.toLowerCase).contains(k.toLowerCase) } ++
        userAgentHeader
    else
      explicitHeaders ++ extraHeaders ++ userAgentHeader
  }
}
