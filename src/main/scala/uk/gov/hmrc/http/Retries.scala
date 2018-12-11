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

package uk.gov.hmrc.http

import akka.actor.ActorSystem
import akka.pattern.after
import javax.net.ssl.SSLException
import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Retries {
  def retry[A](verb: String, url: String)(block: => Future[A]): Future[A]
}

trait NoRetries extends Retries {
  def retry[A](verb: String, url: String)(block: => Future[A]): Future[A] = block
}

trait AkkaRetries extends Retries {

  def actorSystem: ActorSystem

  implicit lazy val ec: ExecutionContext = actorSystem.dispatcher

  def retry[A](verb: String, url: String)(block: => Future[A]): Future[A] =
    block.recoverWith {
      case ex: SSLException if ex.getMessage == "SSLEngine closed already" =>
        Logger.warn(s"Retrying $verb $url due to 'SSLEngine closed already' error")
        after(1.millisecond, actorSystem.scheduler)(retry(verb, url)(block))
    }

}
