/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.play.http.logging

import org.slf4j.MDC

import scala.concurrent.ExecutionContext

@deprecated("MdcLoggingExecutionContext no longer required, please inject Play's default EC instead", "15.6.0")
class MdcLoggingExecutionContext(wrapped: ExecutionContext, mdcData: Map[String, String])
  extends ExecutionContext {

  def execute(runnable: Runnable): Unit =
    wrapped.execute(new RunWithMDC(runnable, mdcData))

  private class RunWithMDC(runnable: Runnable, mdcData: Map[String, String]) extends Runnable {
    def run(): Unit = {
      val oldMdcData = MDC.getCopyOfContextMap
      MDC.clear()
      mdcData.foreach {
        case (k, v) => MDC.put(k, v)
      }
      try {
        runnable.run()
      } finally {
        if (oldMdcData == null)
          MDC.clear()
        else
          MDC.setContextMap(oldMdcData)
      }
    }
  }

  def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}
