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

import play.api.libs.json
import play.twirl.api.Html
import uk.gov.hmrc.play.http.HttpResponse

trait BackwardsCompatibleReadsRecipes extends HtmlHttpReads with JsonHttpReads with ErrorHttpReads with OptionHttpReads with RawHttpReads {
  implicit val readToHtml: HttpReads[Html] =
    convertFailuresToExceptions or bodyToHtml

  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] =
    convertFailuresToExceptions or jsonBodyDeserialisedTo[O]
}
object BackwardsCompatibleReadsRecipes extends BackwardsCompatibleReadsRecipes {
  implicit val readRaw: HttpReads[HttpResponse] = convertFailuresToExceptions or returnTheResponse
}
