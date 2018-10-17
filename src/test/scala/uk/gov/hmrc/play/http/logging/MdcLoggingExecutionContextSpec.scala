/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.concurrent.{CountDownLatch, Executors}

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import ch.qos.logback.core.AppenderBase
import org.scalatest._
import org.slf4j.{LoggerFactory, MDC}
import play.core.NamedThreadFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect._

class MdcLoggingExecutionContextSpec
    extends WordSpecLike
    with Matchers
    with LoneElement
    with Inspectors
    with BeforeAndAfter {

  before {
    MDC.clear()
  }

  "The MDC Transporting Execution Context" should {

    "capture the an MDC map with values in it and put it in place when a task is run" in withCaptureOfLoggingFrom[
      MdcLoggingExecutionContextSpec] { logList =>
      implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map("someKey" -> "something"))

      logEventInsideAFutureUsing(ec)

      logList.loneElement._2 should contain("someKey" -> "something")
    }

    "ignore an null MDC map" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] { logList =>
      implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map())

      logEventInsideAFutureUsing(ec)

      logList.loneElement._2 should be(empty)
    }

    "clear the MDC map after a task is run" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] { logList =>
      implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map("someKey" -> "something"))

      doSomethingInsideAFutureButDontLog(ec)

      MDC.clear()
      logEventInsideAFutureUsing(ec)

      logList.loneElement._2 should be(Map("someKey" -> "something"))
    }

    "clear the MDC map after a task throws an exception" in withCaptureOfLoggingFrom[MdcLoggingExecutionContextSpec] {
      logList =>
        implicit val ec = createAndInitialiseMdcTransportingExecutionContext(Map("someKey" -> "something"))

        throwAnExceptionInATaskOn(ec)

        MDC.clear()
        logEventInsideAFutureUsing(ec)

        logList.loneElement._2 should be(Map("someKey" -> "something"))
    }

    "log values from given MDC map when multiple threads are using it concurrently by ensuring each log from each thread has been logged via MDC" in withCaptureOfLoggingFrom[
      MdcLoggingExecutionContextSpec] { logList =>
      val threadCount = 10
      val logCount    = 10

      val concurrentThreadsEc =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadCount, new NamedThreadFactory("LoggerThread")))
      val startLatch      = new CountDownLatch(threadCount)
      val completionLatch = new CountDownLatch(threadCount)

      for (t <- 0 until threadCount) {
        Future {
          MDC.clear()
          startLatch.countDown()
          startLatch.await()

          for (l <- 0 until logCount) {
            val mdc = Map("entry" -> s"${Thread.currentThread().getName}-$l")
            logEventInsideAFutureUsing(new MdcLoggingExecutionContext(ExecutionContext.global, mdc))
          }

          completionLatch.countDown()
        }(concurrentThreadsEc)
      }

      completionLatch.await()

      val logs = logList.map(_._2).map(_.head._2).toSet
      logs.size should be(threadCount * logCount)

      for (t <- 1 until threadCount) {
        for (l <- 0 until logCount) {
          logs should contain(s"LoggerThread-$t-$l")
        }
      }
    }
  }

  def createAndInitialiseMdcTransportingExecutionContext(mdcData: Map[String, String]): MdcLoggingExecutionContext = {
    val ec = new MdcLoggingExecutionContext(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)), mdcData)
    initialise(ec)
    ec
  }

  def logEventInsideAFutureUsingImplicitEc(implicit ec: ExecutionContext) {
    logEventInsideAFutureUsing(ec)
  }

  def logEventInsideAFutureUsing(ec: ExecutionContext) {
    Await.ready(Future {
      LoggerFactory.getLogger(classOf[MdcLoggingExecutionContextSpec]).info("")
    }(ec), 2 second)
  }

  def doSomethingInsideAFutureButDontLog(ec: ExecutionContext) {
    Await.ready(Future {}(ec), 2 second)
  }

  def throwAnExceptionInATaskOn(ec: ExecutionContext) {
    ec.execute(new Runnable() {
      def run(): Unit =
        throw new RuntimeException("Test what happens when a task running on this EC throws an exception")
    })
  }

  /** Ensures that a thread is already created in the execution context by running an empty future.
    * Required as otherwise the MDC is transferred to the new thread as it is stored in an inheritable
    * ThreadLocal.
    */
  def initialise(ec: ExecutionContext) {
    Await.ready(Future {}(ec), 2 second)
  }

  def withCaptureOfLoggingFrom[T: ClassTag](body: (=> List[(ILoggingEvent, Map[String, String])]) => Any) = {
    val logger = LoggerFactory.getLogger(classTag[T].runtimeClass).asInstanceOf[LogbackLogger]
    val appender = new AppenderBase[ILoggingEvent]() {

      val list = mutable.ListBuffer[(ILoggingEvent, Map[String, String])]()

      override def append(e: ILoggingEvent): Unit =
        list.append((e, e.getMDCPropertyMap().asScala.toMap))
    }
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.ALL)
    logger.setAdditive(true)
    body(appender.list.toList)
  }
}
