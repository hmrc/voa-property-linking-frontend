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

package models.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}

case class UserDetails(
      firstName: Option[String],
      lastName: Option[String],
      email: String,
      postcode: Option[String],
      groupIdentifier: String,
      externalId: String,
      affinityGroup: AffinityGroup,
      credentialRole: CredentialRole,
      confidenceLevel: ConfidenceLevel
)

object UserDetails {

  def fromRetrieval(
        name: Option[Name],
        optEmail: Option[String],
        optPostCode: Option[String],
        groupIdentifier: String,
        externalId: String,
        affinityGroup: AffinityGroup,
        role: CredentialRole,
        confidenceLevel: ConfidenceLevel
  ): UserDetails =
    UserDetails(
      firstName = name.flatMap(_.name),
      lastName = name.flatMap(_.lastName),
      email = optEmail.getOrElse(""),
      postcode = optPostCode,
      groupIdentifier = groupIdentifier,
      externalId = externalId,
      affinityGroup = affinityGroup,
      credentialRole = role,
      confidenceLevel = confidenceLevel
    )

  implicit val format: OFormat[UserDetails] = Json.format

  object IndividualUserDetails {
    def unapply(arg: UserDetails): Boolean = arg.affinityGroup == Individual
  }

  object OrganisationUserDetails {
    def unapply(arg: UserDetails): Boolean = arg.affinityGroup == Organisation
  }

  object AgentUserDetails {
    def unapply(arg: UserDetails): Boolean = arg.affinityGroup == Agent
  }

}
