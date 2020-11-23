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

package uk.gov.hmrc.http.logging

import scala.util.Random
import uk.gov.hmrc.http.{Authorization, ForwardedFor, HeaderNames, RequestChain, RequestId, SessionId}

trait LoggingDetails {

  def sessionId: Option[SessionId]

  def requestId: Option[RequestId]

  def requestChain: RequestChain

  @deprecated("Authorization header is no longer included in logging", "-")
  def authorization: Option[Authorization]

  def forwarded: Option[ForwardedFor]

  def age: Long

  lazy val data: Map[String, Option[String]] = Map(
    HeaderNames.xRequestId    -> requestId.map(_.value),
    HeaderNames.xSessionId    -> sessionId.map(_.value),
    HeaderNames.xForwardedFor -> forwarded.map(_.value)
  )

  def mdcData: Map[String, String] =
    for {
      d <- data
      v <- d._2
    } yield (d._1, v)
}
