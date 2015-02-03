package uk.gov.hmrc.play.http.logging

import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.specs2.specification.BeforeEach
import play.api.Logger
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.UnitSpec
import Mockito._

import scala.util.{Success, Failure}

class ConnectionTracingTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockPlayLogger = mock[Logger]

  val logger = new ConnectionTracing{
    override lazy val connectionLogger = mockPlayLogger
  }

  override def beforeEach() = {
    reset(mockPlayLogger)
  }

  "logResult" should {

    "log 200 as DEBUG" in {

      val ld = new StubLoggingDetails()

      val httpResult = Success("response")

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).debug("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:ok")

    }

    "log 404 error as INFO" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new NotFoundException("not found"))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).info("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed not found")

    }

    "log 404 upstream error as INFO" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Upstream4xxResponse("404 error", 404, 404))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).info("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 404 error")

    }

    "log 401 upstream error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Upstream4xxResponse("401 error", 401, 401))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 401 error")

    }

    "log 400 error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new BadRequestException("400 error"))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 400 error")

    }

    "log 500 upstream error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Upstream5xxResponse("500 error", 500, 500))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 500 error")

    }

    "log 502 error as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new BadGatewayException("502 error"))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed 502 error")

    }

    "log unrecognised exception as WARN" in {

      val ld = new StubLoggingDetails()

      val httpResult = Failure(new Exception("unknown error"))

      logger.logResult(ld, "GET", "/url", 1L)(httpResult)

      verify(mockPlayLogger).warn("RequestId(rId):GET:1:1ns:0:0ns:requestChain:/url:failed unknown error")

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
