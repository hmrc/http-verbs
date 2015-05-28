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

import com.ning.http.util.Base64
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.util.Try

object DeviceFingerprint {

  val deviceFingerprintCookieName = "mdtpdf"

  def deviceFingerprintFrom(request: RequestHeader): String =
    request.cookies.get(deviceFingerprintCookieName).map { cookie =>
      val decodeAttempt = Try {
        Base64.decode(cookie.value)
      }
      decodeAttempt.failed.foreach { e => Logger.info(s"Failed to decode device fingerprint '${cookie.value}' caused by '${e.getClass.getSimpleName}:${e.getMessage}'")}
      decodeAttempt.map {
        new String(_, "UTF-8")
      }.getOrElse("-")
    }.getOrElse("-")

}
