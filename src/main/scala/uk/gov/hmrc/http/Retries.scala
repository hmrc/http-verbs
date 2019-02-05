/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import akka.pattern.after
import com.typesafe.config.Config
import javax.net.ssl.SSLException
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Retries {

  protected def actorSystem: ActorSystem

  protected def configuration: Option[Config]

  implicit lazy val ec: ExecutionContext = actorSystem.dispatcher

  private val logger = LoggerFactory.getLogger("application")

  def retry[A](verb: String, url: String)(block: => Future[A]): Future[A] = {
    def loop(remainingIntervals: Seq[FiniteDuration])(block: => Future[A]): Future[A] =
      block.recoverWith {
        case ex @ `sslEngineClosedMatcher`() if remainingIntervals.nonEmpty =>
          val delay = remainingIntervals.head
          logger.warn(s"Retrying $verb $url in $delay due to '${ex.getMessage}' error")
          after(delay, actorSystem.scheduler)(loop(remainingIntervals.tail)(block))
      }
    loop(intervals)(block)
  }

  private[http] lazy val intervals: Seq[FiniteDuration] = {
    val defaultIntervals = Seq(500.millis, 1.second, 2.seconds, 4.seconds, 8.seconds)
    configuration
      .map { c =>
        val path = "http-verbs.retries.intervals"
        if (c.hasPath(path)) {
          c.getDurationList(path).asScala.map { d =>
            FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)
          }
        } else {
          defaultIntervals
        }
      }
      .getOrElse(defaultIntervals)
  }

  private lazy val sslEngineClosedMatcher =
    new SSlEngineClosedMatcher(isEnabled("ssl-engine-closed-already"))

  private class SSlEngineClosedMatcher(enabled: Boolean) {
    def unapply(ex: Throwable): Boolean =
      ex match {
        case _: SSLException if ex.getMessage == "SSLEngine closed already" => enabled
        case _                                                              => false
      }
  }

  private def isEnabled(name: String): Boolean =
    configuration.exists { c =>
      val path = s"http-verbs.retries.$name.enabled"
      c.hasPath(path) && c.getBoolean(path)
    }

}
