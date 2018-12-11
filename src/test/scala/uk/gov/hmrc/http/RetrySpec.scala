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

package uk.gov.hmrc.http

import java.time.Instant

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.net.ssl.SSLException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Random, Try}
import scala.concurrent.duration._

class RetrySpec extends WordSpec with Matchers with MockitoSugar with ScalaFutures with IntegrationPatience {

  "GET" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {

      trait SucceedNthCall {
        var failureCounter = 0
        val maxFailures    = Random.nextInt(10) + 1
        def process[A, B](success: Future[A], exception: Exception): Future[A] =
          if (failureCounter < maxFailures) {
            failureCounter += 1
            Future.failed(exception)
          } else {
            success
          }
      }

      val httpGet = new HttpGet with AkkaRetries with SucceedNthCall {
        protected def configuration: Option[Config] = None
        override val hooks: Seq[HttpHook]           = Nil
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )

        override val intervals: Seq[FiniteDuration] = List.fill(100)(1.millis)

        override def actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      httpGet.GET[Option[String]](url = "doesnt-matter").futureValue shouldBe None
      httpGet.failureCounter shouldBe httpGet.maxFailures

    }
  }

  "Retries" should {
    "be spread in time as configured" in {

      val expectedIntervals = Seq(300.millis, 500.millis, 750.millis)

      val retries = new AkkaRetries {
        override val intervals   = expectedIntervals
        override val actorSystem = ActorSystem("test-actor-system")
      }

      class KeepFailing {
        var timestamps = List.empty[Instant]
        def process: Future[Unit] = {
          val now = Instant.now
          timestamps = timestamps :+ now
          Future.failed(new SSLException("SSLEngine closed already"))
        }
      }

      val testObject = new KeepFailing

      val _ = Try(retries.retry("GET", "url")(testObject.process).futureValue)

      val actualIntervals: List[Long] =
        testObject.timestamps.sliding(2).toList.map {
          case first :: second :: Nil => second.toEpochMilli - first.toEpochMilli
          case _                      => 0
        }

      actualIntervals.zip(expectedIntervals).foreach {
        case (actual, expected) =>
          actual shouldBe expected.toMillis +- 50 // for error margin as akka scheduler is not very precise
      }

    }
  }

}
