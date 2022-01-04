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

import models.{IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.data.Form
import play.api.data.Forms._

sealed trait AssistantUser extends User {

  val firstName: String
  val lastName: String

  def toIndividualAccountSubmission(fieldData: FieldData)(user: UserDetails)(id: Long)(organisationId: Option[Long]) =
    IndividualAccountSubmission(
      externalId = user.externalId,
      trustId = Some("ASSISTANT_NO_IV"),
      organisationId = organisationId,
      details = IndividualDetails(firstName, lastName, fieldData.email, fieldData.businessPhoneNumber, None, id)
    )

}

object AssistantUser {

  lazy val assistant = Form(
    mapping(
      keys.firstName -> nonEmptyText,
      keys.lastName  -> nonEmptyText
    )(AssistantUserAccountDetails.apply)(AssistantUserAccountDetails.unapply))

}

case class AssistantUserAccountDetails(firstName: String, lastName: String) extends AssistantUser {

  def toGroupDetails(fieldData: FieldData) = GroupAccountDetails(
    companyName = fieldData.businessName,
    address = fieldData.businessAddress,
    email = fieldData.email,
    confirmedEmail = fieldData.email,
    phone = fieldData.businessPhoneNumber,
    isAgent = fieldData.isAgent
  )

}
