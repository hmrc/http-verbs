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

package uk.gov.hmrc.http.hooks

import java.net.URL

import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait HttpHooks {
  val hooks: Seq[HttpHook]

  val NoneRequired = Seq(
    new HttpHook {
      def apply(
        verb     : String,
        url      : URL,
        headers  : Seq[(String, String)],
        body     : Option[HookData],
        responseF: Future[HttpResponse]
      )(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext
     ): Unit = {}
    }
  )

  protected def executeHooks(
    verb   : String,
    url    : URL,
    headers: Seq[(String, String)],
    body   : Option[HookData],
    responseF: Future[HttpResponse]
  )(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit =
    hooks.foreach(_.apply(verb, url, headers, body, responseF))
}
