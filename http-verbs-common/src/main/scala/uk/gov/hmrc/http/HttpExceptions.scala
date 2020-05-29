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

import com.github.ghik.silencer.silent
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

/** Represents an error occuring within the service itself.
  * See [[UpstreamErrorResponse]] for errors returned from Upstream services.
  */
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

class GatewayTimeoutException(message: String) extends HttpException(message, GATEWAY_TIMEOUT)

class HttpVersionNotSupportedException(message: String) extends HttpException(message, HTTP_VERSION_NOT_SUPPORTED)

class InsufficientStorageException(message: String) extends HttpException(message, INSUFFICIENT_STORAGE)


/** Represent unhandled status codes returned from upstream */
// The concrete instances are deprecated, so we can eventually just replace with a case class.
// They should be created via UpstreamErrorResponse.apply and deconstructed via the UpstreamErrorResponse.unapply functions
sealed trait UpstreamErrorResponse extends Exception {
  def message: String

  @deprecated("Use statusCode instead", "11.0.0")
  def upstreamResponseCode: Int

  // final to help migrate away from upstreamResponseCode (i.e. read only - set via UpstreamErrorResponse.apply)
  @silent("deprecated")
  final def statusCode: Int =
    upstreamResponseCode

  def reportAs: Int

  def headers: Map[String, Seq[String]]

  override def getMessage = message
}

@deprecated("Use UpstreamErrorResponse.apply or UpstreamErrorResponse.Upstream4xxResponse.unapply instead.", "11.0.0")
case class Upstream4xxResponse(
  message             : String,
  upstreamResponseCode: Int,
  reportAs            : Int,
  headers             : Map[String, Seq[String]] = Map.empty
) extends UpstreamErrorResponse

@deprecated("Use UpstreamErrorResponse.apply or UpstreamErrorResponse.Upstream5xxResponse.unapply instead.", "11.0.0")
case class Upstream5xxResponse(
  message             : String,
  upstreamResponseCode: Int,
  reportAs            : Int,
  headers             : Map[String, Seq[String]] = Map.empty
) extends UpstreamErrorResponse


object UpstreamErrorResponse {
  def apply(message: String, statusCode: Int): UpstreamErrorResponse =
    apply(
      message    = message,
      statusCode = statusCode,
      reportAs   = statusCode,
      headers    = Map.empty
    )

  def apply(message: String, statusCode: Int, reportAs: Int): UpstreamErrorResponse =
    apply(
      message    = message,
      statusCode = statusCode,
      reportAs   = reportAs,
      headers    = Map.empty
    )

  @silent("deprecated")
  def apply(message: String, statusCode: Int, reportAs: Int, headers: Map[String, Seq[String]]): UpstreamErrorResponse =
    if (statusCode >= 400 && statusCode < 500)
      uk.gov.hmrc.http.Upstream4xxResponse(
        message              = message,
        upstreamResponseCode = statusCode,
        reportAs             = reportAs,
        headers              = headers
      )
    else if (statusCode >= 500 && statusCode < 600)
      uk.gov.hmrc.http.Upstream5xxResponse(
        message              = message,
        upstreamResponseCode = statusCode,
        reportAs             = reportAs,
        headers              = headers
      )
    else throw new IllegalArgumentException(s"Unsupported statusCode $statusCode")

  def unapply(e: UpstreamErrorResponse): Option[(String, Int, Int, Map[String, Seq[String]])] =
    Some((e.message, e.statusCode, e.reportAs, e.headers))

  object Upstream4xxResponse {
    def unapply(e: UpstreamErrorResponse): Option[UpstreamErrorResponse] =
      if (e.statusCode >= 400 && e.statusCode < 500) Some(e) else None
  }

  object Upstream5xxResponse {
    def unapply(e: UpstreamErrorResponse): Option[UpstreamErrorResponse] =
      if (e.statusCode >= 500 && e.statusCode < 600) Some(e) else None
  }

  object WithStatusCode {
    def unapply(e: UpstreamErrorResponse): Option[(Int, UpstreamErrorResponse)] =
      Some((e.statusCode, e))
  }
}