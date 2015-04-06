/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http.reads

import uk.gov.hmrc.play.http.HttpErrorFunctions

trait OptionHttpReads extends HttpErrorFunctions {
  def noneOn(status: Int) = PartialHttpReads[None.type] { (method, url, response) =>
    if (response.status == status) Some(None) else None
  }

  def some[P](implicit rds: HttpReads[P]) = HttpReads[Option[P]] { (method, url, response) =>
    Some(rds.read(method, url, response))
  }

  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] =
    PartialHttpReads.byStatus { case 204 | 404 => None } or some[P]
}
object OptionHttpReads extends OptionHttpReads
