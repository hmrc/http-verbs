package uk.gov.hmrc.play.http.test.logging

import ch.qos.logback.classic.Level
import org.scalatest.{LoneElement, Matchers, WordSpec}
import org.slf4j.LoggerFactory
import play.api.{Logger => PlayLogger}

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
    "Capture a log event from a play logger" in {
      withCaptureOfLoggingFrom(PlayLogger) { logList =>
        PlayLogger.debug("Lonely, I am so lonely...")

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
