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

import akka.dispatch.ExecutorServiceDelegate
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.MDC

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class MdcSpec
  extends AnyWordSpecLike
     with Matchers
     with ScalaFutures
     with IntegrationPatience
     with BeforeAndAfter {

  before {
    MDC.clear()
  }

  "mdcData" should {
    "return a Scala Map" in {
      MDC.put("something1", "something2")
      Mdc.mdcData shouldBe Map("something1" -> "something2")
    }
  }

  "Preserving MDC" should {
    "show that MDC is lost when switching contexts" in {
      implicit val mdcEc: ExecutionContext = mdcPropagatingExecutionContext()

      (for {
        _ <- Future.successful(org.slf4j.MDC.put("k", "v"))
        _ <- runActionWhichLosesMdc()
      } yield
        Option(MDC.get("k"))
      ).futureValue shouldBe None
    }

    "restore MDC" in {
      implicit val mdcEc: ExecutionContext = mdcPropagatingExecutionContext()

      (for {
         _ <- Future.successful(org.slf4j.MDC.put("k", "v"))
         _ <- Mdc.preservingMdc(runActionWhichLosesMdc())
       } yield
        Option(MDC.get("k"))
      ).futureValue shouldBe Some("v")
    }

    "restore MDC when exception is thrown" in {
      implicit val mdcEc: ExecutionContext = mdcPropagatingExecutionContext()

      (for {
         _ <- Future.successful(org.slf4j.MDC.put("k", "v"))
         _ <- Mdc.preservingMdc(runActionWhichLosesMdc(fail = true))
       } yield ()
      ).recover { case _ =>
        Option(MDC.get("k"))
      }.futureValue shouldBe Some("v")
    }
  }

  private def runActionWhichLosesMdc(fail: Boolean = false): Future[Any] = {
    val as = akka.actor.ActorSystem("as")
    akka.pattern.after(10.millis, as.scheduler)(Future(())(as.dispatcher))(as.dispatcher)
      .map(a => if (fail) sys.error("expected test exception") else a)(as.dispatcher)
  }

  private def mdcPropagatingExecutionContext() =
    ExecutionContext.fromExecutor(new MDCPropagatingExecutorService(Executors.newFixedThreadPool(2)))

}

// This is testing with the old bootstrap-play implementation.
// This test is now obsolete with the new (prepare) implementation (as per MdcPropagatingExecutionContext).
class MDCPropagatingExecutorService(val executor: ExecutorService) extends ExecutorServiceDelegate {

  override def execute(command: Runnable): Unit = {
    val mdcData = MDC.getCopyOfContextMap

    executor.execute(() => {
      val oldMdcData = MDC.getCopyOfContextMap
      setMDC(mdcData)
      try {
        command.run()
      } finally {
        // this means any Mdc updates on the ec will not be propagated once it steps out
        setMDC(oldMdcData)
      }
    })
  }

  private def setMDC(context: java.util.Map[String, String]): Unit =
    if (context == null)
      MDC.clear()
    else
      MDC.setContextMap(context)
}
