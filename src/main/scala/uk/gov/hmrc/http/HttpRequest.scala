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

package uk.gov.hmrc.http

import java.net.URL

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.matching.Regex

trait HttpRequest extends Request {

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
