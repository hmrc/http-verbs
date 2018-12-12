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
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Random, Try}

class RetrySpec extends WordSpec with Matchers with MockitoSugar with ScalaFutures with IntegrationPatience {

  "GET" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {

      val http = new HttpGet with SimplifiedHttpVerb {
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.GET[Option[String]](url = "doesnt-matter").futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures

    }
  }

  "DELETE" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new HttpDelete with SimplifiedHttpVerb {
        override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.DELETE[Option[String]](url = "doesnt-matter").futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "PATCH" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new HttpPatch with SimplifiedHttpVerb {
        override def doPatch[A](url: String, body: A)(
          implicit rds: Writes[A],
          hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.PATCH[JsValue, Option[String]](url = "doesnt-matter", Json.obj()).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "PUT" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new HttpPut with SimplifiedHttpVerb {
        override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.PUT[JsValue, Option[String]](url = "doesnt-matter", Json.obj()).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POST" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with SimplifiedHttpVerb {
        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(
          implicit rds: Writes[A],
          hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POST[JsValue, Option[String]](url = "doesnt-matter", Json.obj()).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POSTString" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with SimplifiedHttpVerb {
        override def doPostString(url: String, body: String, headers: Seq[(String, String)])(
          implicit hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POSTString[Option[String]](url = "doesnt-matter", "posted-string").futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POSTForm" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with SimplifiedHttpVerb {
        override def doFormPost(url: String, body: Map[String, Seq[String]])(
          implicit hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POSTForm[Option[String]](url = "doesnt-matter", Map()).futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
    }
  }

  "POSTEmpty" should {
    "retry on SSLException with message 'SSLEngine closed already'" in {
      val http = new TestHttpPost with SimplifiedHttpVerb {
        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
          process(
            success   = Future.successful(HttpResponse(404)),
            exception = new SSLException("SSLEngine closed already")
          )
      }

      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      http.POSTEmpty[Option[String]](url = "doesnt-matter").futureValue shouldBe None
      http.failureCounter shouldBe http.maxFailures
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

  trait SimplifiedHttpVerb extends HttpVerb with AkkaRetries with HttpHooks with SucceedNthCall {
    protected def configuration: Option[Config] = None
    override val hooks: Seq[HttpHook]           = Nil
    override val intervals: Seq[FiniteDuration] = List.fill(1000)(1.millis)
    override def actorSystem: ActorSystem       = ActorSystem("test-actor-system")
  }

  trait SucceedNthCall {
    var failureCounter: Int = 0
    val maxFailures: Int    = Random.nextInt(3) + 1
    def process[A, B](success: Future[A], exception: Exception): Future[A] =
      if (failureCounter < maxFailures) {
        failureCounter += 1
        Future.failed(exception)
      } else {
        success
      }
  }

  trait TestHttpPost extends HttpPost {
    override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(
      implicit wts: Writes[A],
      hc: HeaderCarrier): Future[HttpResponse] = ???

    override def doPostString(url: String, body: String, headers: Seq[(String, String)])(
      implicit hc: HeaderCarrier): Future[HttpResponse] = ???

    override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

    override def doFormPost(url: String, body: Map[String, Seq[String]])(
      implicit hc: HeaderCarrier): Future[HttpResponse] = ???
  }

}
