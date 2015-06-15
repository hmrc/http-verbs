/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.play.audit.model

import play.api.Logger._
import play.api.mvc.RequestHeader


case class DeviceId(id: String, hash: String)

object DeviceId {

  private val CookieName = "mdtpdi"

  // device ID cookie value is made up of [device ID]_[hash of device ID]
  private val DeviceIdPattern = """"?([^_^"]+)_([^_^"]+)"?""".r

  def apply(request: RequestHeader): Option[DeviceId] = request.cookies.get(CookieName) match {
    case Some(cookie) =>
      cookie.value match {
        case DeviceIdPattern(deviceId, hash) =>
          debug(s"Device ID is '$deviceId'")
          Some(DeviceId(deviceId, hash))
        case _ =>
          error(s"Failed to extract device ID from '${cookie.value}'")
          None
      }
    case _ =>
      error(s"Cannot get device ID for this request - cookie '$CookieName' not found")
      None
  }

}
