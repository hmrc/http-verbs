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

package uk.gov.hmrc.play.http.filter

import akka.stream.Materializer
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.slf4j.Logger
import play.api.LoggerLike
import play.api.mvc._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.play.http.DummyRequestHeader
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter

import scala.concurrent.Future

class LoggingFilterSpec extends WordSpecLike with MockitoSugar with Matchers with OptionValues with FutureAwaits with DefaultAwaitTimeout with Eventually {

  class LoggingFilterTest (loggerIn: LoggerLike, controllerNeedsLogging: Boolean)(implicit val mat: Materializer) extends LoggingFilter {
    override def logger = loggerIn
    override def controllerNeedsLogging(controllerName: String): Boolean = controllerNeedsLogging
  }

  "the LoggingFilter should" should {

    def buildFakeLogger() = new LoggerLike {
      var lastInfoMessage: Option[String] = None
      override val logger: Logger = new FakeLogger {
        override def isInfoEnabled = true

        override def info(s: String): Unit = {
          lastInfoMessage = Some(s)
        }
      }
    }

    def requestWith(loggingFilter: LoggingFilter, someTags: Map[String, String] = Map()) = {
      loggingFilter.apply(rh => Future.successful(Results.NoContent))(new DummyRequestHeader() {
        override def tags = someTags
      })
    }

    "log when a requests' path matches a controller which is configured to log" in {
      val fakeLogger = buildFakeLogger()

      implicit val mat: Materializer = mock[Materializer]
      val loggingFilter = new LoggingFilterTest(fakeLogger, true)

      await(requestWith(loggingFilter))

      eventually {
        fakeLogger.lastInfoMessage.value.length should be > 0
      }
    }

    "not log when a requests' path does not match a controller which is not configured to log" in {
      val fakeLogger = buildFakeLogger()

      implicit val mat: Materializer = mock[Materializer]
      val loggingFilter = new LoggingFilterTest(fakeLogger, false)

      await(requestWith(loggingFilter, Map(play.routing.Router.Tags.ROUTE_CONTROLLER -> "exists")))

      fakeLogger.lastInfoMessage shouldBe None
    }
  }
}
