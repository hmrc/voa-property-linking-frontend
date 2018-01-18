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
import models.Address
import play.api.data.Form
import play.api.data.Forms.{email, mapping, nonEmptyText}
import play.api.data.validation.Constraints
import views.helpers.Errors

object CreateEnrolmentOrganisationAccount {
  lazy val keys = new {
    val companyName = "companyName"
    val firstName = "firstName"
    val lastName = "lastName"
    val address = "address"
    val phone = "phone"
    val email = "email"
    val confirmEmail = "confirmedBusinessEmail"
    val isAgent = "isAgent"
  }

  lazy val form = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.companyName -> nonEmptyText(maxLength = 45),
    keys.address -> addressMapping,
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
                                               phone: String,
                                               email: String,
                                               confirmedEmail: String,
                                               isAgent: Boolean)
