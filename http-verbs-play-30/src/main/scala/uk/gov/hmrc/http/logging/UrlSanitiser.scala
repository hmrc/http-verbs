/*
 * Copyright 2024 HM Revenue & Customs
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

object UrlSanitiser {
  def sanitiseForLogging(
    url               : String,
    includeQueryParams: Boolean = false
  ): String = {
    val u           = new java.net.URL(url)
    val userInfo    = Option(u.getUserInfo).filter(_.nonEmpty).fold("")(_ => "<<UserInfo>>@")
    val port        = Option(u.getPort).filter(_ != -1).fold("")(":" + _)
    val queryParams = Option(u.getQuery).filter(_.nonEmpty && includeQueryParams).fold("")("?" + _)
    s"${u.getProtocol}://${userInfo}${u.getHost}${port}${u.getPath}${queryParams}"
  }
}
