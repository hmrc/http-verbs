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

sealed trait Body[+A] {
  final def map[B](f: A => B): Body[B] =
    this match {
      case Body.Complete(body)  => Body.Complete(f(body))
      case Body.Truncated(body) => Body.Truncated(f(body))
    }

  final def isTruncated: Boolean =
    this match {
      case Body.Complete (b) => false
      case Body.Truncated(b) => true
    }
}

object Body {
  case class Complete [A](body: A) extends Body[A]
  case class Truncated[A](body: A) extends Body[A]
}

case class ResponseData(
  body   : Body[String],
  status : Int,
  headers: Map[String, Seq[String]]
)

object ResponseData {
  def fromHttpResponse(httpResponse: HttpResponse) =
    ResponseData(
      body     = Body.Complete(httpResponse.body),
      status   = httpResponse.status,
      headers  = httpResponse.headers
    )
}

case class RequestData(
  headers: Seq[(String, String)],
  body   : Body[Option[HookData]]
)

sealed trait HookData
object HookData {
  case class FromString(s: String)                extends HookData
  case class FromMap(m: Map[String, Seq[String]]) extends HookData
}
