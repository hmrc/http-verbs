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

import java.net.URL

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.hmrc.http.logging.LoggingDetails

import scala.util.matching.Regex

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
) extends LoggingDetails {

  /**
    * @return the time, in nanoseconds, since this header carrier was created
    */
  def age = System.nanoTime() - nsStamp

  val names = HeaderNames

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

  @deprecated("Provide extra headers to VERB (GET, POST) functions", "13.0.0")
  def withExtraHeaders(headers: (String, String)*): HeaderCarrier =
    this.copy(extraHeaders = extraHeaders ++ headers)

  def headers(names: Seq[String]): Seq[(String, String)] =
    (explicitHeaders ++ otherHeaders).filter { case (k, _) => names.map(_.toLowerCase).contains(k.toLowerCase) }

  def headersForUrl(config: HeaderCarrier.Config)(url: String): Seq[(String, String)] = {
    val isInternalHost = config.internalHostPatterns.exists(_.pattern.matcher(new URL(url).getHost).matches())

    if (isInternalHost)
      headers(HeaderNames.explicitlyIncludedHeaders ++ config.headersAllowlist) ++
        config.userAgent.map("User-Agent" -> _).toSeq ++
        extraHeaders
    else
      config.userAgent.map("User-Agent" -> _).toSeq ++
        extraHeaders
  }
}

object HeaderCarrier {
  case class Config(
    internalHostPatterns    : Seq[Regex]     = Seq.empty,
    headersAllowlist        : Seq[String]    = Seq.empty,
    userAgent               : Option[String] = None
  )

  object Config {
    private val logger: Logger = LoggerFactory.getLogger(getClass)

    def fromConfig(config: com.typesafe.config.Config): Config = {
      import scala.collection.JavaConverters.iterableAsScalaIterableConverter

      if (config.hasPath("httpHeadersWhitelist"))
        logger.warn("Use of configuration key 'httpHeadersWhitelist' will be IGNORED. Use 'bootstrap.http.headersAllowlist' instead")

      Config(
        internalHostPatterns     = config.getStringList("internalServiceHostPatterns"            ).asScala.toSeq.map(_.r),
        headersAllowlist         = config.getStringList("bootstrap.http.headersAllowlist"        ).asScala.toSeq,
        userAgent                = if (config.hasPath("appName")) Some(config.getString("appName")) else None
      )
    }
  }
}
