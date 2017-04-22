/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.http.logging

import uk.gov.hmrc.play.http.HeaderNames

import scala.util.Random

case class Authorization(value: String) extends AnyVal

case class SessionId(value: String) extends AnyVal

case class RequestId(value: String) extends AnyVal

case class AkamaiReputation(value: String) extends AnyVal

case class RequestChain(value: String) extends AnyVal {
  def extend = RequestChain(s"$value-${RequestChain.newComponent}")
}

object RequestChain {
  def newComponent = (Random.nextInt & 0xffff).toHexString

  def init = RequestChain(newComponent)
}

case class ForwardedFor(value: String) extends AnyVal

trait LoggingDetails {

  def sessionId: Option[SessionId]

  def requestId: Option[RequestId]

  def requestChain: RequestChain

  def authorization: Option[Authorization]

  def forwarded: Option[ForwardedFor]

  def age: Long

  lazy val data = Map[String, Option[String]](
    (HeaderNames.xRequestId, requestId.map(_.value)),
    (HeaderNames.xSessionId, sessionId.map(_.value)),
    (HeaderNames.authorisation, authorization.map(_.value)),
    (HeaderNames.xForwardedFor, forwarded.map(_.value)))

  def mdcData: Map[String, String] = for {
    d <- data
    v <- d._2
  } yield (d._1, v)
}
