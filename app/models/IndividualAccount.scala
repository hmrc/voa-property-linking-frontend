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

package models

import play.api.libs.json.Json

case class IndividualAccountSubmission(externalId: String, trustId: String, organisationId: Option[Int], details: IndividualDetails)
object IndividualAccountSubmission {
  implicit def formats = Json.format[IndividualAccountSubmission]
}

case class IndividualAccount(externalId: String, trustId: String, organisationId: Int, details: IndividualDetails)

case class DetailedIndividualAccount(externalId: String, trustId: String, organisationId: Int, individualId: Int, details: IndividualDetails) {
  def toIndividualAccount: IndividualAccount = IndividualAccount(externalId, trustId, organisationId, details)
}


object IndividualAccount {
  implicit def formats = Json.format[IndividualAccount]
}
object DetailedIndividualAccount {
  implicit def formats = Json.format[DetailedIndividualAccount]
}
