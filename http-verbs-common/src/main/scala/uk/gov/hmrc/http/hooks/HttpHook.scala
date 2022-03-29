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

package uk.gov.hmrc.http.hooks

import java.net.URL

import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

// TODO requires major version change of http-verbs/play-auditing/bootstrap-play

trait HttpHook {
  def apply(
    verb     : String,
    url      : URL,
    request  : RequestData,
    responseF: Future[ResponseData]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit
}

case class Payload[A](
  body       : A,
  isTruncated: Boolean = false,
  isOmitted  : Boolean = false
)

case class ResponseData(
  payload: Payload[String],
  status : Int,
  headers: Map[String, Seq[String]]
)

object ResponseData {
  def fromHttpResponse(httpResponse: HttpResponse) =
    ResponseData(
      payload         = Payload(httpResponse.body, isTruncated = false, isOmitted = false),
      status          = httpResponse.status,
      headers         = httpResponse.headers
    )
}

case class RequestData(
  headers: Seq[(String, String)], // This doesn't match response type: Map[String, Seq[String]] ...
  payload: Payload[Option[HookData]]
)

sealed trait HookData
object HookData {
  case class FromString(s: String) extends HookData
  case class FromMap(m: Map[String, Seq[String]]) extends HookData
}
