/*
 * Copyright 2015 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpecLike}
import play.api.Logger
import uk.gov.hmrc.play.http._

import scala.util.{Failure, Success}

class ConnectionTracingTest extends WordSpecLike with Matchers {

  def captureLogger() = new Logger(null) {
    var capturedMessage : String = ""

    override def debug(message: => String): Unit = capturedMessage = message

    override def info(message: => String): Unit = capturedMessage = message

    override def warn(message: => String): Unit = capturedMessage = message
  }

  def connectionTracing = new ConnectionTracing{
    override lazy val connectionLogger = captureLogger()
  }

  "logResult" should {

    "log 200 as DEBUG" in {

      val ld = new StubLoggingDetails()

      val httpResult = Success("response")

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:ok"
    }

    "log 404 error as INFO" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new NotFoundException("not found"))


      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed not found"
    }

    "log 404 upstream error as INFO" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Upstream4xxResponse("404 error", 404, 404))

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 404 error"

    }

    "log 401 upstream error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Upstream4xxResponse("401 error", 401, 401))

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 401 error"
    }

    "log 400 error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new BadRequestException("400 error"))

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 400 error"

    }

    "log 500 upstream error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Upstream5xxResponse("500 error", 500, 500))

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 500 error"
    }

    "log 502 error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new BadGatewayException("502 error"))

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 502 error"

    }

    "log unrecognised exception as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Exception("unknown error"))

      val ct = connectionTracing
      ct.logResult(ld, "GET", "/url", 1L)(httpResult)

      ct.connectionLogger.capturedMessage shouldBe "RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed unknown error"
    }



  }

  private class StubLoggingDetails extends LoggingDetails {
    override def sessionId: Option[SessionId] = Some(SessionId("sId"))

    override def forwarded: Option[ForwardedFor] = None

    override def requestId: Option[RequestId] = Some(RequestId("rId"))

    override def age: Long = 1L

    override def authorization: Option[Authorization] = None

    override def requestChain: RequestChain = new RequestChain("requestChain")
  }

}
