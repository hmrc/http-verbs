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

import scala.util.Random

package object logging {

  @deprecated("Use uk.gov.hmrc.http.Authorization instead", "2.12.0")
  type Authorization = uk.gov.hmrc.http.Authorization

  @deprecated("Use uk.gov.hmrc.http.SessionId instead", "2.12.0")
  type SessionId = uk.gov.hmrc.http.SessionId

  @deprecated("Use uk.gov.hmrc.http.SessionId instead", "2.12.0")
  object SessionId {
    def apply(value: String) = uk.gov.hmrc.http.SessionId(value)
  }

  @deprecated("Use uk.gov.hmrc.http.RequestId instead", "2.12.0")
  type RequestId = uk.gov.hmrc.http.RequestId

  @deprecated("Use uk.gov.hmrc.http.RequestId instead", "2.12.0")
  object RequestId {
    def apply(value: String) = uk.gov.hmrc.http.RequestId(value)
  }

  @deprecated("Use uk.gov.hmrc.http.AkamaiReputation instead", "2.12.0")
  type AkamaiReputation = uk.gov.hmrc.http.AkamaiReputation

  @deprecated("Use uk.gov.hmrc.http.AkamaiReputation instead", "2.12.0")
  object AkamaiReputation {
    def apply(value: String) = uk.gov.hmrc.http.AkamaiReputation(value)
  }

  @deprecated("Use uk.gov.hmrc.http.RequestChain instead", "2.12.0")
  type RequestChain = uk.gov.hmrc.http.RequestChain

  @deprecated("Use uk.gov.hmrc.http.RequestChain instead", "2.12.0")
  object RequestChain {
    def apply(value: String) = uk.gov.hmrc.http.RequestChain(value)

    def init = uk.gov.hmrc.http.RequestChain.init
  }

  @deprecated("Use uk.gov.hmrc.http.ForwardedFor instead", "2.12.0")
  type ForwardedFor = uk.gov.hmrc.http.ForwardedFor

  @deprecated("Use uk.gov.hmrc.http.ForwardedFor instead", "2.12.0")
  object ForwardedFor {
    def apply(value: String) = uk.gov.hmrc.http.ForwardedFor(value)
  }
}

package logging {
  trait LoggingDetails {

    import uk.gov.hmrc.http._

    def sessionId: Option[uk.gov.hmrc.http.SessionId]

    def requestId: Option[uk.gov.hmrc.http.RequestId]

    def requestChain: uk.gov.hmrc.http.RequestChain

    @deprecated("Authorization header is no longer included in logging", "-")
    def authorization: Option[uk.gov.hmrc.http.Authorization]

    def forwarded: Option[uk.gov.hmrc.http.ForwardedFor]

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
}