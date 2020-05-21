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

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class HttpException(val message: String, val responseCode: Int) extends Exception(message)

//400s
@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class BadRequestException(message: String) extends HttpException(message, BAD_REQUEST)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class UnauthorizedException(message: String) extends HttpException(message, UNAUTHORIZED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class PaymentRequiredException(message: String) extends HttpException(message, PAYMENT_REQUIRED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class ForbiddenException(message: String) extends HttpException(message, FORBIDDEN)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class NotFoundException(message: String) extends HttpException(message, NOT_FOUND)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class MethodNotAllowedException(message: String) extends HttpException(message, METHOD_NOT_ALLOWED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class NotAcceptableException(message: String) extends HttpException(message, NOT_ACCEPTABLE)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class ProxyAuthenticationRequiredException(message: String)
    extends HttpException(message, PROXY_AUTHENTICATION_REQUIRED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class RequestTimeoutException(message: String) extends HttpException(message, REQUEST_TIMEOUT)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class ConflictException(message: String) extends HttpException(message, CONFLICT)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class GoneException(message: String) extends HttpException(message, GONE)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class LengthRequiredException(message: String) extends HttpException(message, LENGTH_REQUIRED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class PreconditionFailedException(message: String) extends HttpException(message, PRECONDITION_FAILED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class RequestEntityTooLargeException(message: String) extends HttpException(message, REQUEST_ENTITY_TOO_LARGE)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class RequestUriTooLongException(message: String) extends HttpException(message, REQUEST_URI_TOO_LONG)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class UnsupportedMediaTypeException(message: String) extends HttpException(message, UNSUPPORTED_MEDIA_TYPE)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class RequestRangeNotSatisfiableException(message: String)
    extends HttpException(message, REQUESTED_RANGE_NOT_SATISFIABLE)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class ExpectationFailedException(message: String) extends HttpException(message, EXPECTATION_FAILED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class UnprocessableEntityException(message: String) extends HttpException(message, UNPROCESSABLE_ENTITY)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class LockedException(message: String) extends HttpException(message, LOCKED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class FailedDependencyException(message: String) extends HttpException(message, FAILED_DEPENDENCY)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class TooManyRequestException(message: String) extends HttpException(message, TOO_MANY_REQUEST)

//500s
@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class InternalServerException(message: String) extends HttpException(message, INTERNAL_SERVER_ERROR)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class NotImplementedException(message: String) extends HttpException(message, NOT_IMPLEMENTED)

// thrown by HttpErrorFunctions on ConnectException
@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class BadGatewayException(message: String) extends HttpException(message, BAD_GATEWAY)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class ServiceUnavailableException(message: String) extends HttpException(message, SERVICE_UNAVAILABLE)

// thrown by HttpErrorFunctions on TimeoutException
@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class GatewayTimeoutException(message: String) extends HttpException(message, GATEWAY_TIMEOUT)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class HttpVersionNotSupportedException(message: String) extends HttpException(message, HTTP_VERSION_NOT_SUPPORTED)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
class InsufficientStorageException(message: String) extends HttpException(message, INSUFFICIENT_STORAGE)

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
sealed trait UpstreamErrorResponse extends Throwable {
  val message: String
  val upstreamResponseCode: Int
  val reportAs: Int
}

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
case class Upstream4xxResponse(
  message: String,
  upstreamResponseCode: Int,
  reportAs: Int, // TODO remove this - i.e. no context on how it should be reported - move to bootstrap error handler
  headers: Map[String, Seq[String]] = Map.empty) // why not add headers to all UpstreamErrorResponse?
    extends Exception(message) with UpstreamErrorResponse

@deprecated("Use UnhandledHttpException instead.", "11.0.0")
case class Upstream5xxResponse(message: String, upstreamResponseCode: Int, reportAs: Int)
  extends Exception(message) with UpstreamErrorResponse


// A simplified exception model - avoids overlapping exceptions
// --------------------------------------------------------------

case class UnhandledStatusCodeException(
  message   : String,
  statusCode: Int,
  reportAs  : Int,
  headers   : Map[String, Seq[String]]
) extends Exception(message)


object UnhandledStatusCodeException {
  def apply(message: String, statusCode: Int): UnhandledStatusCodeException =
    UnhandledStatusCodeException(
      message    = message,
      statusCode = statusCode,
      reportAs   = statusCode,
      headers    = Map.empty
    )

  def apply(message: String, statusCode: Int, reportAs: Int): UnhandledStatusCodeException =
    UnhandledStatusCodeException(
      message    = message,
      statusCode = statusCode,
      reportAs   = reportAs,
      headers    = Map.empty
    )

  object Upstream4xxResponse {
    def unapply(e: UnhandledStatusCodeException): Option[UnhandledStatusCodeException] =
      if (e.statusCode >= 400 && e.statusCode < 500) Some(e) else None
  }

  object Upstream5xxResponse {
    def unapply(e: UnhandledStatusCodeException): Option[UnhandledStatusCodeException] =
      if (e.statusCode >= 500) Some(e) else None
  }

  object WithStatusCode {
    def unapply(e: UnhandledStatusCodeException): Option[(Int, UnhandledStatusCodeException)] =
      Some((e.statusCode, e))
  }
}

object Test {
  UnhandledStatusCodeException("BadRequest", 400, 400, Map.empty) match {
    case e: UnhandledStatusCodeException if (e.statusCode == 400) => println(s"msg=${e.message}, code = ${e.statusCode}")
    case UnhandledStatusCodeException.WithStatusCode(400, e)      => println(s"msg=${e.message}, code = ${e.statusCode}")
    case UnhandledStatusCodeException.Upstream4xxResponse(e)      => println(s"msg=${e.message}, code = ${e.statusCode}")
    case UnhandledStatusCodeException.Upstream5xxResponse(e)      => println(s"msg=${e.message}, code = ${e.statusCode}")
    case UnhandledStatusCodeException(message, statusCode, reportAs, headers) => println(s"msg=$message, code = $statusCode")
  }
}