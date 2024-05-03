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

package uk.gov.hmrc.play.http.ws

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HttpResponse

object WSHttpResponse {
  def apply(wsResponse: WSResponse): HttpResponse =
    HttpResponse(
      status  = wsResponse.status,
      body    = wsResponse.body,
      headers = forScala2_13(wsResponse.headers)
    )

  // play returns scala.collection.Seq, but default for Scala 2.13 is scala.collection.immutable.Seq
  // duplicated from CollectionUtils since WSHttpResponse is not defined within uk.gov.hmrc.http package..
  private def forScala2_13(m: Map[String, scala.collection.Seq[String]]): Map[String, Seq[String]] =
    // `m.mapValues(_.toSeq).toMap` by itself strips the ordering away
    scala.collection.immutable.TreeMap[String, Seq[String]]()(scala.math.Ordering.comparatorToOrdering(String.CASE_INSENSITIVE_ORDER)) ++ m.view.mapValues(_.toSeq)
}
