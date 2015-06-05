/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.play.audit.filters

import play.api.libs.iteratee.Iteratee
import play.api.mvc.{EssentialAction, Results, Result, RequestHeader}

import scala.concurrent.ExecutionContext

trait FilterFlowMock {

  def action(implicit ec: ExecutionContext) : (RequestHeader) => Iteratee[Array[Byte], Result] = { requestHeader =>
    Iteratee.fold[Array[Byte], Result](new Results.Status(404)) {
      (length, bytes) => new Results.Status(200)
    }
  }

  def nextAction(implicit ec: ExecutionContext) = EssentialAction(action)


}
