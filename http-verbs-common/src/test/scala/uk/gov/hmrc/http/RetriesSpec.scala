/*
 * Copyright 2021 HM Revenue & Customs
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
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import javax.net.ssl.SSLException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.slf4j.MDC
import _root_.play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.http.logging.Mdc

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Try}

class RetriesSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with ScalaFutures
     with IntegrationPatience {
  import ExecutionContext.Implicits.global

  "Retries.retryOnSslEngineClosed" should {
    "be disabled by default" in {
      val retries: Retries = new Retries {
        override protected val configuration: Config = ConfigFactory.load()
        override val actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      @volatile var counter = 0
      val resultF =
        retries.retryOnSslEngineClosed("GET", "url") {
          Future.failed {
            counter += 1
            new SSLException("SSLEngine closed already")
          }
        }

      whenReady(resultF.failed) { e =>
        e       shouldBe an[SSLException]
        counter shouldBe 1
      }
    }

    "have configurable intervals" in {
      val retries: Retries = new Retries {
        override protected val configuration: Config =
            ConfigFactory.parseString(
              "http-verbs.retries.intervals = [ 100 ms, 200 ms,  1 s]"
            )
        override val actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      retries.intervals shouldBe Seq(100.millis, 200.millis, 1.second)
    }

    "run a successful future only once" in {
      val retries: Retries = new Retries {
        override protected val configuration = ConfigFactory.load()
        override val actorSystem             = ActorSystem("test-actor-system")
      }

      @volatile var counter = 0
      val resultF =
        retries.retryOnSslEngineClosed("GET", "url") {
          Future.successful {
            counter += 1
            counter
          }
        }

      whenReady(resultF) { c =>
        c shouldBe 1
      }
    }

    "be spread in time" in {
      val expectedIntervals = Seq(300.millis, 500.millis, 750.millis)

      val retries: Retries = new Retries {
        override protected val configuration: Config =
          ConfigFactory.parseString("http-verbs.retries.ssl-engine-closed-already.enabled = true")
        override private[http] lazy val intervals = expectedIntervals
        override val actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      @volatile var timestamps = List.empty[Instant]
      def failingFuture: Future[Unit] = {
        val now = Instant.now
        timestamps = timestamps :+ now
        Future.failed(new SSLException("SSLEngine closed already"))
      }

      val _ = Try(retries.retryOnSslEngineClosed("GET", "url")(failingFuture).futureValue)

      val actualIntervals: List[Long] =
        timestamps.sliding(2).toList.map {
          case first :: second :: Nil => second.toEpochMilli - first.toEpochMilli
          case _                      => 0
        }

      actualIntervals.zip(expectedIntervals).foreach {
        case (actual, expected) =>
          actual shouldBe expected.toMillis +- 50 // for error margin as akka scheduler is not very precise
      }
    }

    "eventually return a failure for a Future that will never succeed" in {
      val expectedIntervals = Seq(300.millis, 500.millis, 750.millis)

      val retries: Retries = new Retries {
        override protected val configuration: Config =
          ConfigFactory.parseString("http-verbs.retries.ssl-engine-closed-already.enabled = true")
        override private[http] lazy val intervals = expectedIntervals
        override val actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      val resultF =
        retries.retryOnSslEngineClosed("GET", "url") {
          Future.failed {
            new SSLException("SSLEngine closed already")
          }
        }

      whenReady(resultF.failed) { e =>
        e shouldBe an[SSLException]
      }

    }

    "return a success for a Future that eventually succeeds" in {
      val expectedIntervals = Seq(300.millis, 500.millis, 750.millis)

      val retries: Retries with SucceedNthCall = new Retries with SucceedNthCall {
        override protected val configuration: Config =
          ConfigFactory.parseString("http-verbs.retries.ssl-engine-closed-already.enabled = true")
        override private[http] lazy val intervals = expectedIntervals
        override val actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      val expectedResponse = HttpResponse(404, "")
      val resultF =
        retries.retryOnSslEngineClosed("GET", "url") {
          retries.failFewTimesAndThenSucceed(
            success   = Future.successful(expectedResponse),
            exception = new SSLException("SSLEngine closed already")
          )
        }

      whenReady(resultF) { response =>
        response shouldBe expectedResponse
      }
    }

    "preserve MDC" in {
      val expectedIntervals = Seq(300.millis, 500.millis, 750.millis)

      val retries: Retries with SucceedNthCall = new Retries with SucceedNthCall {
        override protected val configuration: Config =
          ConfigFactory.parseString("http-verbs.retries.ssl-engine-closed-already.enabled = true")
        override private[http] lazy val intervals = expectedIntervals
        override val actorSystem: ActorSystem = ActorSystem("test-actor-system")
      }

      val mdcData = Map("key1" -> "value1")

      implicit val mdcEc = ExecutionContext.fromExecutor(new uk.gov.hmrc.play.http.logging.MDCPropagatingExecutorService(Executors.newFixedThreadPool(2)))

      val expectedResponse = HttpResponse(404, "")

      val resultF =
        for {
          _   <- Future.successful(Mdc.putMdc(mdcData))
          res <- retries.retryOnSslEngineClosed("GET", "url") {
                  // assert mdc available to block execution
                  Option(MDC.getCopyOfContextMap).map(_.asScala.toMap).getOrElse(Map.empty) shouldBe mdcData

                  retries.failFewTimesAndThenSucceed(
                    success   = Future.successful(expectedResponse),
                    exception = new SSLException("SSLEngine closed already")
                  )
                }
        } yield {
          // assert mdc available to continuation
          Option(MDC.getCopyOfContextMap).map(_.asScala.toMap).getOrElse(Map.empty) shouldBe mdcData
          res
        }

      whenReady(resultF) { response =>
        response shouldBe expectedResponse
      }
    }
  }

  "GET" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new HttpGet with TestHttpVerb with SucceedNthCall {
        override def doGet(url: String, headers: Seq[(String, String)])(
          implicit ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.GET[Option[String]](url = "http://doesnt-matter", Seq("header" -> "foo")).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "DELETE" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new HttpDelete with TestHttpVerb with SucceedNthCall {
        override def doDelete(url: String, headers: Seq[(String, String)])(
          implicit ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.DELETE[Option[String]](url = "https://www.google.co.uk", headers = Seq("header" -> "foo")).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "PATCH" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new HttpPatch with TestHttpVerb with SucceedNthCall {
        override def doPatch[A](url: String, body: A, headers: Seq[(String, String)])(
          implicit rds: Writes[A], ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http
        .PATCH[JsValue, Option[String]](url = "https://www.google.co.uk", Json.obj(), Seq("header" -> "foo"))
        .futureValue      shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "PUT" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPut with TestHttpVerb with SucceedNthCall {
        override def doPut[A](url: String, body: A, headers: Seq[(String, String)])(
          implicit rds: Writes[A], ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.PUT[JsValue, Option[String]](url = "https://www.google.co.uk", Json.obj()).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POST" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with TestHttpVerb with SucceedNthCall {
        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(
          implicit rds: Writes[A],
          ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POST[JsValue, Option[String]](url = "https://www.google.co.uk", Json.obj()).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POSTString" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with TestHttpVerb with SucceedNthCall {
        override def doPostString(url: String, body: String, headers: Seq[(String, String)])(
          implicit ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POSTString[Option[String]](url = "https://www.google.co.uk", "posted-string").futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POSTForm" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with TestHttpVerb with SucceedNthCall {
        override def doFormPost(
          url: String,
          body: Map[String, Seq[String]],
          headers: Seq[(String, String)])(
            implicit ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POSTForm[Option[String]](url = "https://www.google.co.uk", Map.empty[String, Seq[String]], Seq.empty[(String, String)]).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POSTEmpty" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with TestHttpVerb with SucceedNthCall {
        override def doEmptyPost[A](
          url: String,
          headers: Seq[(String, String)])(
            implicit ec: ExecutionContext): Future[HttpResponse] =
          failFewTimesAndThenSucceed(
            success   = Future.successful(HttpResponse(404, "")),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POSTEmpty[Option[String]](url = "https://www.google.co.uk").futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  trait TestHttpVerb extends HttpVerb with Retries with HttpHooks {
    System.setProperty("akka.jvm-shutdown-hooks", "off")
    protected def configuration: Config =
      ConfigFactory.parseString("http-verbs.retries.ssl-engine-closed-already.enabled = true")
        .withFallback(ConfigFactory.load())
    override val hooks: Seq[HttpHook]                              = Nil
    override private[http] lazy val intervals: Seq[FiniteDuration] = List.fill(3)(1.millis)
    override def actorSystem: ActorSystem                          = ActorSystem("test-actor-system")
  }

  trait SucceedNthCall {
    var failureCounter: Int = 0
    val maxFailures: Int    = 1 + Random.nextInt(2)
    def failFewTimesAndThenSucceed[A, B](success: Future[A], exception: Exception): Future[A] = {
      println(s"failFewTimesAndThenSucceed = $failureCounter, $maxFailures")
      if (failureCounter < maxFailures) {
        failureCounter += 1
        Future.failed(exception)
      } else
        success
    }
  }

  trait TestHttpPost extends HttpPost {
    override def doPost[A](
      url: String,
      body: A,
      headers: Seq[(String, String)])(
        implicit wts: Writes[A],
      ec: ExecutionContext): Future[HttpResponse] =
      ???

    override def doPostString(
      url: String,
      body: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      ???

    override def doEmptyPost[A](
      url: String,
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      ???

    override def doFormPost(
      url: String,
      body: Map[String, Seq[String]],
      headers: Seq[(String, String)])(
        implicit ec: ExecutionContext): Future[HttpResponse] =
      ???
  }

  trait TestHttpPut extends HttpPut {
    def doPutString(url: String, body: String, headers: Seq[(String, String)])(
      implicit ec: ExecutionContext): Future[HttpResponse] = ???
  }
}
