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

package uk.gov.hmrc.http

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.annotation.implicitNotFound

package client {
  trait Streaming
}
package object client
  extends StreamHttpReadsInstances {

  // ensures strict HttpReads are not passed to stream function, which would lead to stream being read into memory
  // (or runtime exceptions since HttpResponse.body with throw exception for streamed responses)
  @implicitNotFound("""Could not find an implicit StreamHttpReads[${A}].
    You may be missing an implicit Materializer.""")
  type StreamHttpReads[A] = HttpReads[A] with Streaming
}

trait StreamHttpReadsInstances {
  def tag[A](instance: A): A with client.Streaming =
    instance.asInstanceOf[A with client.Streaming]

  implicit val readStreamHttpResponse: client.StreamHttpReads[HttpResponse] =
    tag[HttpReads[HttpResponse]](
      HttpReads.ask.map { case (_, _, response) => response }
    )

  implicit def readStreamEitherHttpResponse(implicit mat: Materializer, errorTimeout: ErrorTimeout): client.StreamHttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
    tag[HttpReads[Either[UpstreamErrorResponse, HttpResponse]]](
      HttpReads.ask.flatMap { case (method, url, response) =>
        HttpErrorFunctions.handleResponseEitherStream(method, url)(response) match {
          case Left(err)       => HttpReads.pure(Left(err))
          case Right(response) => HttpReads.pure(Right(response))
        }
      }
    )

  implicit def readEitherSource(implicit mat: Materializer, errorTimeout: ErrorTimeout): client.StreamHttpReads[Either[UpstreamErrorResponse, Source[ByteString, _]]] =
    tag[HttpReads[Either[UpstreamErrorResponse, Source[ByteString, _]]]](
      readStreamEitherHttpResponse
         .map(_.map(_.bodyAsSource))
    )

  implicit def readSource(implicit mat: Materializer, errorTimeout: ErrorTimeout): client.StreamHttpReads[Source[ByteString, _]] =
    tag[HttpReads[Source[ByteString, _]]](
      readEitherSource
        .map {
          case Left(err)    => throw err
          case Right(value) => value
        }
    )
}
