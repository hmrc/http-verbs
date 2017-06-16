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

package uk.gov.hmrc.play.http.hooks

import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

trait HttpHook {
  def apply(url: String, verb: String, body: Option[_], responseF: Future[HttpResponse])(implicit hc: HeaderCarrier)
}

trait HttpHooks {
  val hooks: Seq[HttpHook]

  val NoneRequired = Seq(new HttpHook {
    def apply(url: String, verb: String, body: Option[_], responseF: Future[HttpResponse])(implicit hc: HeaderCarrier) = {}
  })

  protected def executeHooks(url: String, verb: String, body: Option[_], responseF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {
    hooks.foreach(hook => hook(url, verb, body, responseF))
  }
}
