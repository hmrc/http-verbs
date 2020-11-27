/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => is}
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.hooks.HttpHook

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._

class HttpDeleteSpec extends AnyWordSpecLike with Matchers with MockitoSugar with CommonHttpBehaviour {

  import ExecutionContext.Implicits.global

  class StubbedHttpDelete(doDeleteResult: Future[HttpResponse], doDeleteWithHeaderResult: Future[HttpResponse]) extends HttpDelete with ConnectionTracingCapturing {
    val testHook1                                   = mock[HttpHook]
    val testHook2                                   = mock[HttpHook]
    val hooks                                       = Seq(testHook1, testHook2)
    override val configuration: Config              = ConfigFactory.load()
    override protected val actorSystem: ActorSystem = ActorSystem("test-actor-system")

    def appName: String = ???

    def doDelete(url: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
      doDeleteResult
  }

  "HttpDelete" should {
    "be able to return plain responses" in {
      val response   = HttpResponse(200, testBody)
      val testDelete = new StubbedHttpDelete(Future.successful(response), Future.successful(response))
      testDelete.DELETE[HttpResponse](url, Seq("foo" -> "bar")).futureValue shouldBe response
    }

    "be able to return objects deserialised from JSON" in {
      val testDelete = new StubbedHttpDelete(Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")),
        Future.successful(HttpResponse(200, """{"foo":"t","bar":10}""")))
      testDelete
        .DELETE[TestClass](url, Seq("foo" -> "bar"))
        .futureValue(Timeout(Span(2, Seconds)), Interval(Span(15, Millis))) shouldBe TestClass("t", 10)
    }

    behave like anErrorMappingHttpCall("DELETE", (url, responseF) => new StubbedHttpDelete(responseF, responseF).DELETE[HttpResponse](url, Seq("foo" -> "bar")))
    behave like aTracingHttpCall("DELETE", "DELETE", new StubbedHttpDelete(defaultHttpResponse, defaultHttpResponse)) { _.DELETE[HttpResponse](url, Seq("foo" -> "bar")) }

    "Invoke any hooks provided" in {
      val dummyResponse       = HttpResponse(200, testBody)
      val dummyResponseFuture = Future.successful(dummyResponse)
      val dummyHeader         = Future.successful(dummyResponse)
      val testDelete          = new StubbedHttpDelete(dummyResponseFuture, dummyHeader)

      testDelete.DELETE[HttpResponse](url, Seq("header" -> "foo")).futureValue

      val respArgCaptor1 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])
      val respArgCaptor2 = ArgumentCaptor.forClass(classOf[Future[HttpResponse]])

      verify(testDelete.testHook1).apply(is(url), is("DELETE"), is(None), respArgCaptor1.capture())(any(), any())
      verify(testDelete.testHook2).apply(is(url), is("DELETE"), is(None), respArgCaptor2.capture())(any(), any())

      // verifying directly without ArgumentCaptor didn't work as Futures were different instances
      // e.g. Future.successful(5) != Future.successful(5)
      respArgCaptor1.getValue.futureValue shouldBe dummyResponse
      respArgCaptor2.getValue.futureValue shouldBe dummyResponse
    }
  }
}
