/*
 * Copyright 2022 HM Revenue & Customs
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

trait HttpHook {
  def apply(
    verb     : String,
    url      : URL,
    headers  : Seq[(String, String)],
    body     : Option[HookData],
    responseF: Future[HttpResponse]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit
}

sealed trait HookData
object HookData {
  // TODO won' be binary compatible with play-auditing...
  case class FromString(s: String, isTruncated: Boolean) extends HookData
  case class FromMap(m: Map[String, Seq[String]]) extends HookData
}
