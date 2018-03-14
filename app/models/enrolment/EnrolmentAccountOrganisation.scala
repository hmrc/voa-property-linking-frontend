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

import form.Mappings._
import form.TextMatching
import models.{Address, IVDetails}
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{email, mapping, nonEmptyText, text}
import play.api.data.validation._
import uk.gov.hmrc.domain.Nino
import views.helpers.Errors

object CreateEnrolmentOrganisationAccount {

  lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) = {
    Nino(nino.toUpperCase.replaceAll(" ", ""))
  }

  lazy val keys = new {
    val companyName = "companyName"
    val firstName = "firstName"
    val lastName = "lastName"
    val address = "address"
    val phone = "phone"
    val email = "email"
    val confirmEmail = "confirmedBusinessEmail"
    val isAgent = "isAgent"
    val dateOfBirth = "dob"
    val nino = "nino"
  }

  lazy val form = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.companyName -> nonEmptyText(maxLength = 45),
    keys.address -> addressMapping,
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino,
    keys.phone -> nonEmptyText.verifying("Maximum length is 15", _.length <= 15),
    keys.email -> email.verifying(Constraints.maxLength(150)),
    keys.confirmEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.isAgent -> mandatoryBoolean
  )(EnrolmentOrganisationAccountDetails.apply)(EnrolmentOrganisationAccountDetails.unapply))
}

case class CreateEnrolmentOrganisationAccountVM(form: Form[_])

case class EnrolmentOrganisationAccountDetails(firstName: String,
                                               lastName: String,
                                               companyName: String,
                                               address: Address,
                                               dob: LocalDate,
                                               nino: Nino,
                                               phone: String,
                                               email: String,
                                               confirmedEmail: String,
                                               isAgent: Boolean) {

  def toIvDetails = IVDetails(firstName, lastName, dob, nino)
}
