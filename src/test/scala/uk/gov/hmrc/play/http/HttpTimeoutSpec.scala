package uk.gov.hmrc.play.http

import java.net.{ServerSocket, URI}
import java.util.concurrent.TimeoutException

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.webbitserver.handler.{DelayedHttpHandler, StringHttpHandler}
import org.webbitserver.netty.NettyWebServer
import play.api.Play
import play.api.test.FakeApplication
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.UnitSpec


import scala.concurrent.ExecutionContext.Implicits.global

class HttpTimeoutSpec extends UnitSpec with ScalaFutures with BeforeAndAfterAll {


  override def beforeAll() {
    super.beforeAll()
    val fakeApplication = FakeApplication(additionalConfiguration = Map("ws.timeout.request" -> "1000"))
    Play.start(fakeApplication)
  }

  override def afterAll() {
    super.afterAll()
    Play.stop()
  }


  "HttpCalls" should {

    "be gracefully timeout when no response is received within the 'timeout' frame" in {
      val http = new WSHttp with MockAuditing

      // get an unused port
      val ss = new ServerSocket(0)
      ss.close()
      val publicUri = URI.create(s"http://localhost:${ss.getLocalPort}")
      val ws = new NettyWebServer(global, ss.getLocalSocketAddress, publicUri)
      try {
        //starts web server
        ws.add("/test", new DelayedHttpHandler(global, 2000, new StringHttpHandler("application/json", "{name:'pong'}")))
        ws.start().get()
        
        implicit val hc = HeaderCarrier()
        
        val start= System.currentTimeMillis()
        intercept[TimeoutException] {
          //make request to web server
          await(http.doPost(s"$publicUri/test", "{name:'ping'}", Seq()))
        }
        val diff  = (System.currentTimeMillis() - start).toInt
        // there is test execution delay around 700ms
        diff should be >= 1000
        diff should be < 2500

      } finally {
        ws.stop()
      }

    }

  }

}
