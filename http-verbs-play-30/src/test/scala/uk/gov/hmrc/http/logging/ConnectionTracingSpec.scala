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

package uk.gov.hmrc.http.logging

import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.slf4j.Logger
import uk.gov.hmrc.http._

import scala.util.{Failure, Success}

class ConnectionTracingSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with BeforeAndAfterEach {

  val mockPlayLogger = mock[Logger]

  val connectionTracing = new ConnectionTracing {
    override lazy val connectionLogger = mockPlayLogger
  }

  override def beforeEach() =
    reset(mockPlayLogger)

  "logResult" should {
    "log 200 as DEBUG" in {
      val ld = new StubLoggingDetails()

      val httpResult = Success("response")

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).debug("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:ok")
    }

    "log 404 error as INFO" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(new NotFoundException("not found"))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).info("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed not found")
    }

    "log 404 upstream error as INFO" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(UpstreamErrorResponse(message = "404 error", statusCode = 404, reportAs = 404))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).info("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 404 error")
    }

    "log 401 upstream error as WARN" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(UpstreamErrorResponse(message = "401 error", statusCode = 401, reportAs = 401))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 401 error")
    }

    "log 400 error as WARN" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(UpstreamErrorResponse(message = "400 error", statusCode = 400, reportAs = 400))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 400 error")
    }

    "log 500 upstream error as WARN" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(UpstreamErrorResponse(message = "500 error", statusCode = 500, reportAs = 500))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 500 error")
    }

    "log 502 error as WARN" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(new BadGatewayException("502 error"))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 502 error")
    }

    "log unrecognised exception as WARN" in {
      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Exception("unknown error"))

      connectionTracing.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed unknown error")
    }
  }

  private class StubLoggingDetails extends LoggingDetails {
    import uk.gov.hmrc.http.{Authorization, ForwardedFor, RequestId, RequestChain, SessionId}

    override def sessionId: Option[SessionId] = Some(SessionId("sId"))

    override def forwarded: Option[ForwardedFor] = None

    override def requestId: Option[RequestId] = Some(RequestId("rId"))

    override def age: Long = 1L

    override def authorization: Option[Authorization] = None

    override def requestChain: RequestChain = new RequestChain("requestChain")
  }
}
