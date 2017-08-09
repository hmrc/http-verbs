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

package uk.gov.hmrc.http.test.logging

import ch.qos.logback.classic.Level
import org.scalatest.{LoneElement, Matchers, WordSpec}
import org.slf4j.LoggerFactory

class LogCapturingSpec extends  WordSpec with Matchers with LogCapturing with LoneElement {

  "A test which captures the logs" should {
    "Not capture anything if there was no logging" in withCaptureOfLoggingFrom[LogCapturingSpec] { logList =>
      logList should be(empty)
    }
    "Not capture logging from other classes" in withCaptureOfLoggingFrom[LogCapturingSpec] { logList =>
      TopLevelClassWhichDoesSomeLogging().doLogging()
      logList should be(empty)
    }
    "Capture a log event from the captured class" in {
      withCaptureOfLoggingFrom[TopLevelClassWhichDoesSomeLogging] { logList =>
        TopLevelClassWhichDoesSomeLogging().doLogging()

        logList.loneElement should (
          have('level(Level.DEBUG)) and
          have('message("Lonely, I am so lonely..."))
        )
      }
    }
    "Capture a log event from a logger" in {

      lazy val connectionLogger = LoggerFactory.getLogger("connector")
      withCaptureOfLoggingFrom(connectionLogger) { logList =>
        connectionLogger.debug("Lonely, I am so lonely...")

        logList.loneElement should (
          have('level(Level.DEBUG)) and
          have('message("Lonely, I am so lonely..."))
        )
      }
    }
  }
}

case class TopLevelClassWhichDoesSomeLogging() {
  def doLogging() { LoggerFactory.getLogger(this.getClass).debug("Lonely, I am so lonely...") }
}
