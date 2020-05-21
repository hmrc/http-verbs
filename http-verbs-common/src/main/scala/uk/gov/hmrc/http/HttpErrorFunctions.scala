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

trait HttpErrorFunctions {
  def notFoundMessage(verbName: String, url: String, responseBody: String): String =
    s"$verbName of '$url' returned 404 (Not Found). Response body: '$responseBody'"

  def preconditionFailedMessage(verbName: String, url: String, responseBody: String): String =
    s"$verbName of '$url' returned 412 (Precondition Failed). Response body: '$responseBody'"

  def upstreamResponseMessage(verbName: String, url: String, status: Int, responseBody: String): String =
    s"$verbName of '$url' returned $status. Response body: '$responseBody'"

  def badRequestMessage(verbName: String, url: String, responseBody: String): String =
    s"$verbName of '$url' returned 400 (Bad Request). Response body '$responseBody'"

  def badGatewayMessage(verbName: String, url: String, status: Int, responseBody: String): String =
    s"$verbName of '$url' returned status $status. Response body: '$responseBody'"

  def badGatewayMessage(verbName: String, url: String, e: Exception): String =
    s"$verbName of '$url' failed. Caused by: '${e.getMessage}'"

  def gatewayTimeoutMessage(verbName: String, url: String, e: Exception): String =
    s"$verbName of '$url' timed out with message '${e.getMessage}'"

  def is2xx(status: Int) = status >= 200 && status < 300

  def is4xx(status: Int) = status >= 400 && status < 500

  def is5xx(status: Int) = status >= 500 && status < 600

  @deprecated("Use handleReponseEither instead.", "11.0.0")
  def handleResponse(httpMethod: String, url: String)(response: HttpResponse): HttpResponse =
    response.status match {
      case status if is2xx(status) => response
      case 400                     => throw new BadRequestException(badRequestMessage(httpMethod, url, response.body))
      case 404                     => throw new NotFoundException(notFoundMessage(httpMethod, url, response.body))
      case status if is4xx(status) =>
        throw new Upstream4xxResponse(
          upstreamResponseMessage(httpMethod, url, status, response.body),
          status,
          500,
          response.allHeaders)
      case status if is5xx(status) =>
        throw new Upstream5xxResponse(upstreamResponseMessage(httpMethod, url, status, response.body), status, 502)
      case status =>
        throw new Exception(s"$httpMethod to $url failed with status $status. Response body: '${response.body}'")
    }

  // Note, no special handling of BadRequest or NotFound
  // they will be returned as `Left(Upstream4xxResponse(status = 400))` and `Left(Upstream4xxResponse(status = 404))` respectively
  def handleResponseEither(httpMethod: String, url: String)(response: HttpResponse): Either[UnhandledStatusCodeException, HttpResponse] =
    response.status match {
      case status if is4xx(status) || is5xx(status) =>
        Left(UnhandledStatusCodeException(
          message    = upstreamResponseMessage(httpMethod, url, status, response.body),
          statusCode = status,
          reportAs   = if (is4xx(status)) HttpExceptions.INTERNAL_SERVER_ERROR else HttpExceptions.BAD_GATEWAY,
          headers    = response.allHeaders
        ))
      // Note all cases not handled above (e.g. 1xx, 2xx and 3xx) will be returned as is
      // default followRedirect should mean we don't see 3xx...
      case status  => Right(response)
    }
}

object HttpErrorFunctions extends HttpErrorFunctions
