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

import scala.collection.generic.CanBuildFrom
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

final case class Data[+A](value: A, isTruncated: Boolean) {

  def map[B](f: A => B): Data[B] =
    flatMap(a => Data.pure(f(a)))

  def map2[B, C](data: Data[B])(f: (A, B) => C): Data[C] =
    flatMap(a => data.map(b => f(a, b)))

  def flatMap[B](f: A => Data[B]): Data[B] =
    if (isTruncated) Data(f(value).value, isTruncated = true) else f(value)
}

object Data {

  def pure[A](value: A): Data[A] =
    Data(value, isTruncated = false)

  def truncated[A](value: A): Data[A] =
    Data(value, isTruncated = true)

  def traverse[A, B, M[X] <: TraversableOnce[X]](in: M[A])(f: A => Data[B])(
    implicit cbf: CanBuildFrom[M[A], B, M[B]]
  ): Data[M[B]] =
    in.foldLeft(Data.pure(cbf(in)))((acc, x) => acc.map2(f(x))(_ += _)).map(_.result())
}

case class ResponseData(
  body   : Data[String],
  status : Int,
  headers: Map[String, Seq[String]]
)

object ResponseData {
  def fromHttpResponse(httpResponse: HttpResponse) =
    ResponseData(
      body     = Data.pure(httpResponse.body),
      status   = httpResponse.status,
      headers  = httpResponse.headers
    )
}

case class RequestData(
  headers: Seq[(String, String)],
  body   : Option[Data[HookData]]
)

sealed trait HookData
object HookData {
  case class FromString(s: String)                extends HookData
  case class FromMap(m: Map[String, Seq[String]]) extends HookData
}
