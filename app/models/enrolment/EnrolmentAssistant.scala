/*
 * Copyright 2018 HM Revenue & Customs
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

package models.enrolment

import form.Mappings._
import form.TextMatching
import models.{Address, IVDetails, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.data.Forms._
import play.api.data.validation.{Constraints, _}
import play.api.data.{Form, Mapping}
import views.helpers.Errors


trait EnrolmentAssistant {

  val firstName: String
  val lastName: String
  val address: Address
  val phone: String
  val email: String
  val confirmedEmail: String

  def toIndividualAccountSubmission(user: UserDetails)(id: Long)(organisationId: Option[Long]) = IndividualAccountSubmission(
    externalId = user.externalId,
    trustId = "NONIV",
    organisationId = organisationId,
    details = IndividualDetails(firstName, lastName, email, phone, None, id)
  )

}

object EnrolmentAssistant {

  lazy val assistant = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.companyName -> nonEmptyText(maxLength = 45),
    keys.address -> addressMapping,
    keys.phone -> nonEmptyText.verifying("Maximum length is 15", _.length <= 15),
    keys.email -> email.verifying(Constraints.maxLength(150)),
    keys.confirmedBusinessEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.isAgent -> mandatoryBoolean
  )(EnrolmentAssistantAccountDetails.apply)(EnrolmentAssistantAccountDetails.unapply))

}
