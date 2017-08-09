/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.http.logging

import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.{HttpException, Upstream4xxResponse}

import scala.concurrent._
import scala.util.{Failure, Success, Try}

trait ConnectionTracing {

  lazy val connectionLogger = LoggerFactory.getLogger("connector")

  def withTracing[T](method: String, uri: String)(body: => Future[T])(implicit ld: LoggingDetails, ec: ExecutionContext): Future[T] = {
    val startAge = ld.age
    val f = body
    f.onComplete(logResult(ld, method, uri, startAge))
    f
  }

  def logResult[A](ld: LoggingDetails, method: String, uri: String, startAge: Long)(result: Try[A]) = result match {
    case Success(ground) => connectionLogger.debug(formatMessage(ld, method, uri, startAge, "ok"))
    case Failure(ex: HttpException) if ex.responseCode == 404 => connectionLogger.info(formatMessage(ld, method, uri, startAge, s"failed ${ex.getMessage}"))
    case Failure(ex: Upstream4xxResponse) if ex.upstreamResponseCode == 404 => connectionLogger.info(formatMessage(ld, method, uri, startAge, s"failed ${ex.getMessage}"))
    case Failure(ex) => connectionLogger.warn(formatMessage(ld, method, uri, startAge, s"failed ${ex.getMessage}"))
  }

  import uk.gov.hmrc.http.logging.ConnectionTracing.formatNs

  def formatMessage(ld: LoggingDetails, method: String, uri: String, startAge: Long, message: String) = {
    val requestId = ld.requestId.getOrElse("")
    val requestChain = ld.requestChain
    val durationNs = ld.age - startAge
    s"$requestId:$method:$startAge:${formatNs(startAge)}:$durationNs:${formatNs(durationNs)}:${requestChain.value}:$uri:$message"
  }
}

object ConnectionTracing {
  def formatNs(ns: Long): String = {
    val nsPart = ns % 1000
    val usPart = ns / 1000 % 1000
    val msPart = ns / 1000000 % 1000
    val sPart = ns / 1000000000

    if (sPart > 0) f"${(sPart * 1000 + msPart) / 1000.0}%03.3fs"
    else if (msPart > 0) f"${(msPart * 1000 + usPart) / 1000.0}%03.3fms"
    else if (usPart > 0) f"${(usPart * 1000 + nsPart) / 1000.0}%03.3fus"
    else s"${ns}ns"
  }
}
