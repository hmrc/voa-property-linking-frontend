/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors.authorisation.errorhandler.exceptions

import uk.gov.hmrc.http.HttpException

class BraAuthorisationFailure(message: String, responseCode: Int) extends HttpException(message, responseCode)

object BraAuthorisationFailure {

  def apply(message: String, respondeCode: Int = 401): BraAuthorisationFailure =
    new BraAuthorisationFailure(message, respondeCode)
}