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

package uk.gov.hmrc.http.play

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, StringContextOps}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


/** Demonstrates usage */
object Example {
  import scala.concurrent.ExecutionContext.Implicits.global

  // this would be injected
  val httpClient2: HttpClient2 = {
    implicit val as: ActorSystem = ActorSystem("test-actor-system")
    val config = Configuration(ConfigFactory.load())
    val wsClient: WSClient = {
      implicit val mat: Materializer = ActorMaterializer() // explicitly required for play-26
      AhcWSClient(AhcWSClientConfigFactory.forConfig(config.underlying))
    }
    new HttpClient2Impl(
      wsClient,
      as,
      config,
      hooks = Seq.empty, // this would be wired up to play-auditing
    )
  }

  // json
  {
    implicit val hc = HeaderCarrier()

    val _: Future[ResDomain] =
      httpClient2
        .put(url"http://localhost:8000/")
        .withBody(toJson(ReqDomain()))
        .withProxy
        .replaceHeader("User-Agent" -> "ua")

        .execute(fromJson[ResDomain])
  }


  // streams
  {
    implicit val hc = HeaderCarrier()

    val srcStream: Source[ByteString, _] = ???

    val _: Future[Source[ByteString, _]] =
      httpClient2
        //.put(url"http://localhost:8000/", srcStream)
        .put(url"http://localhost:8000/")
        .withBody(srcStream)
        .transform(_.withRequestTimeout(10.seconds))
        .stream(fromStream)
  }

  // retries
  {
    val retries = new Retries {
      override val actorSystem   = ActorSystem("test-actor-system")
      override val configuration = ConfigFactory.load()
    }

    implicit val hc = HeaderCarrier()

    val _: Future[ResDomain] =
      retries.retryFor("get reqdomain"){ case UpstreamErrorResponse.WithStatusCode(502) => true }{
        httpClient2
          .put(url"http://localhost:8000/")
          .withBody(toJson(ReqDomain()))
          .withProxy
          .replaceHeader("User-Agent" -> "ua")
          .execute(fromJson[ResDomain])
      }
  }
}

case class ReqDomain()

object ReqDomain {
  implicit val w: Writes[ReqDomain] = ???
}

case class ResDomain()

object ResDomain {
  implicit val r : Reads[ResDomain]     = ???
  implicit val hr: HttpReads[ResDomain] = ???
}
