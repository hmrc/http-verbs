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

package uk.gov.hmrc.http

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import org.slf4j.LoggerFactory
import uk.gov.hmrc.mdc.Mdc

import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

trait Retries {

  protected def actorSystem: ActorSystem

  protected def configuration: Config

  private val logger = LoggerFactory.getLogger("application")

  private lazy val sslRetryEnabled =
    configuration.getBoolean("http-verbs.retries.ssl-engine-closed-already.enabled")

  def retryOnSslEngineClosed[A](verb: String, url: String)(block: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    retryFor(s"$verb $url") { case ex: SSLException if ex.getMessage == "SSLEngine closed already" => sslRetryEnabled }(block)

  @deprecated("Use retryOnSslEngineClosed instead", "14.0.0")
  def retry[A](verb: String, url: String)(block: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    retryOnSslEngineClosed(verb, url)(block)

  def retryFor[A](
    label    : String
  )(condition: PartialFunction[Exception, Boolean]
  )(block    : => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] = {
    def loop(remainingIntervals: Seq[FiniteDuration]): Future[A] = {
      // scheduling will loose MDC data. Here we explicitly ensure it is available on block.
      block
        .recoverWith {
          case ex: Exception if condition.lift(ex).getOrElse(false) && remainingIntervals.nonEmpty =>
            val delay = remainingIntervals.head
            logger.warn(s"Retrying $label in $delay due to error: ${ex.getMessage}")
            val mdcData = Mdc.mdcData
            after(delay, actorSystem.scheduler){
              Mdc.putMdc(mdcData)
              loop(remainingIntervals.tail)
            }
        }
      }
    loop(intervals)
  }

  private[http] lazy val intervals: Seq[FiniteDuration] =
    configuration.getDurationList("http-verbs.retries.intervals").asScala.toSeq.map { d =>
      FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS)
    }
}
