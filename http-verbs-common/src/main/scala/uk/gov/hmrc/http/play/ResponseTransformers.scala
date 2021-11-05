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

package uk.gov.hmrc.http.play

import akka.stream.scaladsl.Source
import akka.util.ByteString
import _root_.play.api.libs.ws.WSRequest
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HttpResponse, HttpReads}

import java.util.concurrent.TimeoutException
import java.net.ConnectException
import scala.concurrent.{ExecutionContext, Future}

trait ResponseTransformers {
  def fromStream(
    request  : WSRequest,
    responseF: Future[HttpResponse]
  )(implicit
    ec: ExecutionContext
  ): Future[Source[ByteString, _]] =
    mapErrors(request, responseF)
      .map(_.bodyAsSource)

  def withHttpReads[A](
    request: WSRequest,
    responseF: Future[HttpResponse]
  )(implicit
    r : HttpReads[A],
    ec: ExecutionContext
  ): Future[A] =
    mapErrors(request, responseF)
      .map(r.read(request.method, request.url, _))

  def mapErrors(
    request  : WSRequest,
    responseF: Future[HttpResponse]
  )(implicit
    ec: ExecutionContext
  ): Future[HttpResponse] =
    responseF.recoverWith {
      case e: TimeoutException => Future.failed(new GatewayTimeoutException(s"${request.method} of '${request.url}' timed out with message '${e.getMessage}'"))
      case e: ConnectException => Future.failed(new BadGatewayException(s"${request.method} of '${request.url}' failed. Caused by: '${e.getMessage}'"))
    }
}
