/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.http.test

import play.api.libs.json.Writes
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

object Concurrent {
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._

  val defaultTimeout = 5 seconds

  implicit def extractAwait[A](future: Future[A]) = await[A](future)
  implicit def liftFuture[A](v: A) = Future.successful(v)

  def await[A](future: Future[A]) = Await.result(future, defaultTimeout)
}

trait TestHttpTransport extends HttpTransport {
  override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier):Future[HttpResponse]= ???

  override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
}

