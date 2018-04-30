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

import java.time.LocalDate

import controllers.GroupAccountDetails
import form.Mappings._
import form.TextMatching
import models.{Address, IVDetails, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.data.Forms._
import play.api.data.validation.{Constraints, _}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.domain.Nino
import views.helpers.Errors


trait EnrolmentUser {

  val firstName: String
  val lastName: String
  val address: Address
  val dob: LocalDate
  val nino: Nino
  val phone: String
  val email: String
  val confirmedEmail: String

  def toIndividualAccountSubmission(user: UserDetails)(id: Int)(organisationId: Option[Long]) = IndividualAccountSubmission(
    externalId = user.externalId,
    trustId = "NONIV",
    organisationId = organisationId,
    details = IndividualDetails(firstName, lastName, email, phone, None, id)
  )

  def toGroupDetails: GroupAccountDetails

  def toIvDetails = IVDetails(
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dob,
    nino = nino
  )
}

object EnrolmentUser {

  lazy val keys = new {
    val companyName = "companyName"
    val firstName = "firstName"
    val lastName = "lastName"
    val address = "address"
    val phone = "phone"
    val mobilePhone = "mobilePhone"
    val email = "email"
    val confirmedEmail = "confirmedEmail"
    val confirmedBusinessEmail = "confirmedBusinessEmail"
    val tradingName = "tradingName"
    val dateOfBirth = "dob"
    val nino = "nino"
    val isAgent = "isAgent"
  }

  lazy val individual = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.address -> addressMapping,
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino,
    keys.phone -> nonEmptyText(maxLength = 15),
    keys.mobilePhone -> nonEmptyText(maxLength = 15),
    keys.email -> email.verifying(Constraints.maxLength(150)),
    keys.confirmedEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.tradingName -> optional(text())
  )(EnrolmentIndividualAccountDetails.apply)(EnrolmentIndividualAccountDetails.unapply))

  lazy val organisation = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.companyName -> nonEmptyText(maxLength = 45),
    keys.address -> addressMapping,
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino,
    keys.phone -> nonEmptyText.verifying("Maximum length is 15", _.length <= 15),
    keys.email -> email.verifying(Constraints.maxLength(150)),
    keys.confirmedBusinessEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.isAgent -> mandatoryBoolean
  )(EnrolmentOrganisationAccountDetails.apply)(EnrolmentOrganisationAccountDetails.unapply))

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) = {
    Nino(nino.toUpperCase.replaceAll(" ", ""))
  }
}