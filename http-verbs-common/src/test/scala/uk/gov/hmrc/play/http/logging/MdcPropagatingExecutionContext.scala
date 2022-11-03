/*
 * Copyright 2022 HM Revenue & Customs
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

// This is (approximately) the new bootstrap-play implementation.
class MdcPropagatingExecutionContext(val ec: ExecutionContext) extends ExecutionContext {
  override def prepare(): ExecutionContext = new ExecutionContext {
    // capture the MDC
    private val mdcContext = MDC.getCopyOfContextMap

    override def execute(r: Runnable): Unit =
      ec.execute { () =>
        // backup the callee MDC context
        val oldMDCContext = MDC.getCopyOfContextMap

        // Run the runnable with the captured context
        setContextMap(mdcContext)
        try {
          r.run()
        } finally {
          // restore the callee MDC context
          setContextMap(oldMDCContext)
        }
      }

    override def reportFailure(t: Throwable): Unit =
      ec.reportFailure(t)
  }

  override def execute(r: Runnable): Unit =
    ec.execute(r)

  override def reportFailure(t: Throwable): Unit =
    ec.reportFailure(t)

  private[this] def setContextMap(context: java.util.Map[String, String]) =
    if (context == null)
      MDC.clear()
    else
      MDC.setContextMap(context)
}
