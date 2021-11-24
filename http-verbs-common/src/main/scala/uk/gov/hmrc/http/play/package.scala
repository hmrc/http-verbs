/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.scaladsl.Source
import akka.util.ByteString

import _root_.play.api.libs.json.{JsValue, Writes}

package object play {
  // ensure strict HttpReads are not passed to stream function, which would lead to stream being read into memory
  trait Streaming
  type StreamHttpReads[A] = HttpReads[A] with Streaming
}

trait StreamHttpReadsInstances {
  def tag[A](instance: A): A with play.Streaming =
    instance.asInstanceOf[A with play.Streaming]

  implicit val readEitherSource: HttpReads[Either[UpstreamErrorResponse, Source[ByteString, _]]] with play.Streaming =
    tag[HttpReads[Either[UpstreamErrorResponse, Source[ByteString, _]]]](
      HttpReads.ask.flatMap { case (method, url, response) =>
        HttpErrorFunctions.handleResponseEither(method, url)(response) match {
          case Left(err)       => HttpReads.pure(Left(err))
          case Right(response) => HttpReads.pure(Right(response.bodyAsSource))
        }
      }
    )

  implicit val readSource: HttpReads[Source[ByteString, _]] with play.Streaming =
    tag[HttpReads[Source[ByteString, _]]](
      readEitherSource
        .map {
          case Left(err)    => throw err
          case Right(value) => value
        }
    )
}

object StreamHttpReadsInstances extends StreamHttpReadsInstances
