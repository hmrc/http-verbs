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

package uk.gov.hmrc.play.http

import java.net.{ServerSocket, URI}
import java.util.concurrent.TimeoutException

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.webbitserver.handler.{DelayedHttpHandler, StringHttpHandler}
import org.webbitserver.netty.NettyWebServer
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import play.api.{Configuration, Play}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.TestHttpCore

import scala.concurrent.ExecutionContext.global

class HttpTimeoutSpec extends WordSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll {

  lazy val fakeApplication =
    GuiceApplicationBuilder(configuration = Configuration("play.ws.timeout.request" -> "1000ms")).build()

  override def beforeAll() {
    super.beforeAll()
    Play.start(fakeApplication)
  }

  override def afterAll() {
    super.afterAll()
    Play.stop(fakeApplication)
  }

  WsTestClient.withClient(client => {

    "HttpCalls" should {

      "be gracefully timeout when no response is received within the 'timeout' frame" in {
        val http = new WSHttp with TestHttpCore {
          override val wsClient = fakeApplication.injector.instanceOf[WSClient]
        }

        // get an unused port
        val ss = new ServerSocket(0)
        ss.close()
        val publicUri = URI.create(s"http://localhost:${ss.getLocalPort}")
        val ws        = new NettyWebServer(global, ss.getLocalSocketAddress, publicUri)
        try {
          //starts web server
          ws.add(
            "/test",
            new DelayedHttpHandler(global, 2000, new StringHttpHandler("application/json", "{name:'pong'}")))
          ws.start().get()

          implicit val hc = HeaderCarrier()

          val start = System.currentTimeMillis()
          intercept[TimeoutException] {
            //make request to web server
            import uk.gov.hmrc.play.test.Concurrent.await
            await(http.doPost(s"$publicUri/test", "{name:'ping'}", Seq()))
          }
          val diff = (System.currentTimeMillis() - start).toInt
          // there is test execution delay around 700ms
          diff should be >= 1000
          diff should be < 2500

        } finally {
          ws.stop()
        }

      }

    }
  })
}
