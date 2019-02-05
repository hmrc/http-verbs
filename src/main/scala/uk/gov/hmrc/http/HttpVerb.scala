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

import java.net.{ConnectException, URL}
import java.util.concurrent.TimeoutException

import com.typesafe.config.Config

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait HttpVerb extends Request {

  protected def configuration: Option[Config]

  @deprecated("ProcessingFunction is obsolete, use the relevant HttpReads[A] instead", "18/03/2015")
  type ProcessingFunction = (Future[HttpResponse], String) => Future[HttpResponse]

  def mapErrors(httpMethod: String, url: String, f: Future[HttpResponse])(
    implicit ec: ExecutionContext): Future[HttpResponse] =
    f.recover {
      case e: TimeoutException => throw new GatewayTimeoutException(gatewayTimeoutMessage(httpMethod, url, e))
      case e: ConnectException => throw new BadGatewayException(badGatewayMessage(httpMethod, url, e))
    }

  def badGatewayMessage(verbName: String, url: String, e: Exception): String =
    s"$verbName of '$url' failed. Caused by: '${e.getMessage}'"

  def gatewayTimeoutMessage(verbName: String, url: String, e: Exception): String =
    s"$verbName of '$url' timed out with message '${e.getMessage}'"

  lazy val internalHostPatterns: Seq[Regex] = configuration match {
    case Some(config) if config.hasPathOrNull("internalServiceHostPatterns") =>
      config.getStringList("internalServiceHostPatterns").asScala.map(_.r).toSeq
    case _ =>
      Seq("^.*\\.service$".r, "^.*\\.mdtp$".r)
  }

  lazy val userAgentHeader: Seq[(String, String)] = configuration match {
    case Some(config) if config.hasPathOrNull("appName") =>
      Seq("User-Agent" -> config.getString("appName"))
    case _ =>
      Seq.empty
  }

  override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val headers = if (internalHostPatterns.exists(_.pattern.matcher(new URL(url).getHost).matches())) {
      hc.headers
    } else {
      hc.headers.filterNot(hc.otherHeaders.contains(_))
    }

    headers ++ userAgentHeader
  }
}
