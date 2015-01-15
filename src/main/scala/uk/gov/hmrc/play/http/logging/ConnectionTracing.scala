package uk.gov.hmrc.play.http.logging

import play.api.Logger
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent._
import scala.util.{Failure, Success, Try}

trait ConnectionTracing {

  lazy val connectionLogger = Logger("connector")

  def withTracing[T](method: String, uri: String)(body: => Future[T])(implicit ld: LoggingDetails): Future[T] = {
    val startAge = ld.age
    val f = body
    f.onComplete(logResult(ld, method, uri, startAge))
    f
  }

  def logResult[A](ld: LoggingDetails, method: String, uri: String, startAge: Long)(result: Try[A]) = result match {
    case Success(ground) => connectionLogger.debug(formatMessage(ld, method, uri, startAge, "ok"))
    case Failure(ex) => connectionLogger.warn(formatMessage(ld, method, uri, startAge, s"failed ${ex.getMessage}"))
  }

  import uk.gov.hmrc.play.http.logging.ConnectionTracing.formatNs

  def formatMessage(ld: LoggingDetails, method: String, uri: String, startAge: Long, message: String) = {
    val requestId = ld.requestId.getOrElse("")
    val requestChain = ld.requestChain
    val durationNs = ld.age - startAge
    s"$requestId:$method:${startAge}:${formatNs(startAge)}:${durationNs}:${formatNs(durationNs)}:${requestChain.value}:$uri:$message"
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

