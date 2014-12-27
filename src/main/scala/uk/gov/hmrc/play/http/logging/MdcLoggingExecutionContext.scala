package uk.gov.hmrc.play.http.logging

import org.slf4j.MDC
import play.api.libs.concurrent.Execution.defaultContext

import scala.concurrent.ExecutionContext

object MdcLoggingExecutionContext {
  implicit def fromLoggingDetails(implicit loggingDetails: LoggingDetails): ExecutionContext =
    new MdcLoggingExecutionContext(defaultContext, loggingDetails.mdcData)
}

class MdcLoggingExecutionContext(wrapped: ExecutionContext, mdcData: Map[String, String]) extends ExecutionContext {

  def execute(runnable: Runnable) {
    wrapped.execute(new RunWithMDC(runnable, mdcData))
  }

  private class RunWithMDC(runnable: Runnable, mdcData: Map[String, String]) extends Runnable {
    def run(): Unit = {
      mdcData.foreach {
        case (k, v) => MDC.put(k, v)
      }
      try {
        runnable.run()
      }
      finally {
        MDC.clear()
      }
    }
  }

  def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}
