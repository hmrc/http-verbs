/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.http.HttpExceptions._

private object HttpExceptions {

  val BAD_REQUEST                     = 400
  val UNAUTHORIZED                    = 401
  val PAYMENT_REQUIRED                = 402
  val FORBIDDEN                       = 403
  val NOT_FOUND                       = 404
  val METHOD_NOT_ALLOWED              = 405
  val NOT_ACCEPTABLE                  = 406
  val PROXY_AUTHENTICATION_REQUIRED   = 407
  val REQUEST_TIMEOUT                 = 408
  val CONFLICT                        = 409
  val GONE                            = 410
  val LENGTH_REQUIRED                 = 411
  val PRECONDITION_FAILED             = 412
  val REQUEST_ENTITY_TOO_LARGE        = 413
  val REQUEST_URI_TOO_LONG            = 414
  val UNSUPPORTED_MEDIA_TYPE          = 415
  val REQUESTED_RANGE_NOT_SATISFIABLE = 416
  val EXPECTATION_FAILED              = 417
  val UNPROCESSABLE_ENTITY            = 422
  val LOCKED                          = 423
  val FAILED_DEPENDENCY               = 424
  val TOO_MANY_REQUEST                = 429

  val INTERNAL_SERVER_ERROR      = 500
  val NOT_IMPLEMENTED            = 501
  val BAD_GATEWAY                = 502
  val SERVICE_UNAVAILABLE        = 503
  val GATEWAY_TIMEOUT            = 504
  val HTTP_VERSION_NOT_SUPPORTED = 505
  val INSUFFICIENT_STORAGE       = 507
}

class HttpException(val message: String, val responseCode: Int) extends Exception(message)

//400s
class BadRequestException(message: String) extends HttpException(message, BAD_REQUEST)

class UnauthorizedException(message: String) extends HttpException(message, UNAUTHORIZED)

class PaymentRequiredException(message: String) extends HttpException(message, PAYMENT_REQUIRED)

class ForbiddenException(message: String) extends HttpException(message, FORBIDDEN)

class NotFoundException(message: String) extends HttpException(message, NOT_FOUND)

class MethodNotAllowedException(message: String) extends HttpException(message, METHOD_NOT_ALLOWED)

class NotAcceptableException(message: String) extends HttpException(message, NOT_ACCEPTABLE)

class ProxyAuthenticationRequiredException(message: String)
    extends HttpException(message, PROXY_AUTHENTICATION_REQUIRED)

class RequestTimeoutException(message: String) extends HttpException(message, REQUEST_TIMEOUT)

class ConflictException(message: String) extends HttpException(message, CONFLICT)

class GoneException(message: String) extends HttpException(message, GONE)

class LengthRequiredException(message: String) extends HttpException(message, LENGTH_REQUIRED)

class PreconditionFailedException(message: String) extends HttpException(message, PRECONDITION_FAILED)

class RequestEntityTooLargeException(message: String) extends HttpException(message, REQUEST_ENTITY_TOO_LARGE)

class RequestUriTooLongException(message: String) extends HttpException(message, REQUEST_URI_TOO_LONG)

class UnsupportedMediaTypeException(message: String) extends HttpException(message, UNSUPPORTED_MEDIA_TYPE)

class RequestRangeNotSatisfiableException(message: String)
    extends HttpException(message, REQUESTED_RANGE_NOT_SATISFIABLE)

class ExpectationFailedException(message: String) extends HttpException(message, EXPECTATION_FAILED)

class UnprocessableEntityException(message: String) extends HttpException(message, UNPROCESSABLE_ENTITY)

class LockedException(message: String) extends HttpException(message, LOCKED)

class FailedDependencyException(message: String) extends HttpException(message, FAILED_DEPENDENCY)

class TooManyRequestException(message: String) extends HttpException(message, TOO_MANY_REQUEST)

//500s
class InternalServerException(message: String) extends HttpException(message, INTERNAL_SERVER_ERROR)

class NotImplementedException(message: String) extends HttpException(message, NOT_IMPLEMENTED)

class BadGatewayException(message: String) extends HttpException(message, BAD_GATEWAY)

class ServiceUnavailableException(message: String) extends HttpException(message, SERVICE_UNAVAILABLE)

@deprecated("use GatewayTimeoutException instead", "0.1.0")
class GatewayTimeout(message: String) extends HttpException(message, GATEWAY_TIMEOUT)

class GatewayTimeoutException(message: String) extends HttpException(message, GATEWAY_TIMEOUT)

class HttpVersionNotSupportedException(message: String) extends HttpException(message, HTTP_VERSION_NOT_SUPPORTED)

class InsufficientStorageException(message: String) extends HttpException(message, INSUFFICIENT_STORAGE)

case class Upstream4xxResponse(
  message: String,
  upstreamResponseCode: Int,
  reportAs: Int,
  headers: Map[String, Seq[String]] = Map.empty)
    extends Exception(message)

case class Upstream5xxResponse(message: String, upstreamResponseCode: Int, reportAs: Int) extends Exception(message)
