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
import akka.pattern.after
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.http.logging.Mdc

trait Retries {

  protected def actorSystem: ActorSystem

  protected def configuration: Config

  private val logger = LoggerFactory.getLogger("application")

  def retry[A](verb: String, url: String)(block: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    def loop(remainingIntervals: Seq[FiniteDuration])(mdcData: Map[String, String])(block: => Future[A]): Future[A] =
      // scheduling will loose MDC data. Here we explicitly ensure it is available on block.
      Mdc.withMdc(block, mdcData)
        .recoverWith {
          case ex @ `sslEngineClosedMatcher`() if remainingIntervals.nonEmpty =>
            val delay = remainingIntervals.head
            logger.warn(s"Retrying $verb $url in $delay due to '${ex.getMessage}' error")
            after(delay, actorSystem.scheduler)(loop(remainingIntervals.tail)(mdcData)(block))
        }
    loop(intervals)(Mdc.mdcData)(block)
  }

  private[http] lazy val intervals: Seq[FiniteDuration] =
    configuration.getDurationList("http-verbs.retries.intervals").asScala.map { d =>
      FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)
    }

  private lazy val sslEngineClosedMatcher =
    new SSlEngineClosedMatcher(
      enabled = configuration.getBoolean("http-verbs.retries.ssl-engine-closed-already.enabled")
    )

  private class SSlEngineClosedMatcher(enabled: Boolean) {
    def unapply(ex: Throwable): Boolean =
      ex match {
        case _: SSLException if ex.getMessage == "SSLEngine closed already" => enabled
        case _                                                              => false
      }
  }
}
