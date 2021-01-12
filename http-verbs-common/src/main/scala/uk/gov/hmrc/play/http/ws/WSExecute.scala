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

package uk.gov.hmrc.play.http.ws

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.http.logging.Mdc

trait WSExecute {

  private[ws] def execute(req: play.api.libs.ws.WSRequest, method: String)(implicit ec: ExecutionContext) = {
    // Since AHC internally uses a different execution context, providing a MDC enabled Execution context
    // will not preserve MDC data for further futures.
    // We will copy over the data manually to preserve them.
    Mdc.preservingMdc {
      req.withMethod(method).execute()
    }
  }
}