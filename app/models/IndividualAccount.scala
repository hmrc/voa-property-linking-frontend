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

package models

import play.api.libs.json.{Json, OFormat}

case class IndividualAccountSubmission(
      externalId: String,
      trustId: Option[String],
      organisationId: Option[Long],
      details: IndividualDetails)
object IndividualAccountSubmission {
  implicit val format: OFormat[IndividualAccountSubmission] = Json.format
}

case class IndividualAccount(
      externalId: String,
      trustId: Option[String],
      organisationId: Long,
      details: IndividualDetails)

case class DetailedIndividualAccount(
      externalId: String,
      trustId: Option[String],
      organisationId: Long,
      individualId: Long,
      details: IndividualDetails) {
  def toIndividualAccount: IndividualAccount = IndividualAccount(externalId, trustId, organisationId, details)
}

object IndividualAccount {
  implicit val format: OFormat[IndividualAccount] = Json.format
}
object DetailedIndividualAccount {
  implicit val format: OFormat[DetailedIndividualAccount] = Json.format
}
