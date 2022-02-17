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

import form.Mappings._
import models.domain.Nino
import models.registration.AdminUser.phoneNumberRegex
import models.{Address, IVDetails, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import utils.EmailAddressValidation

import java.time.LocalDate


case class RegistrationSession (
                                 firstName:      String,
                                 lastName:       String,
                                 address:        Option[Address] = None,
                                 dob:            Option[LocalDate] = None,
                                 nino:           Option[Nino] = None,
                                 phone:          Option[String] = None,
                                 mobilePhone:    Option[String] = None,
                                 tradingName:    Option[String] = None,
                                 email:          Option[String] = None,
                                 confirmedEmail: Option[String] = None
                               )

object RegistrationSession {

  implicit val format = Json.format[RegistrationSession]

  def apply(individualName: IndividualName): RegistrationSession =
    RegistrationSession(
      firstName = individualName.firstName,
      lastName = individualName.lastName
    )

}


case class IndividualName(firstName: String, lastName: String)

object IndividualName {

  lazy val individualNameForm: Form[IndividualName] = Form(
    mapping(
      keys.firstName -> nonEmptyText,
      keys.lastName  -> nonEmptyText
    )(IndividualName.apply)(IndividualName.unapply)
  )

  implicit val format = Json.format[IndividualName]

}

case class IndividualPersonalDetails(address: Address, email: String, phone: String, mobilePhone: String, tradingName: Option[String])

object IndividualPersonalDetails {
  private def validatePhoneNumber = {
    def validPhoneNumberLength(num: String) =
      num.length >= 11 && num.length <= 20

    text
      .verifying("error.phoneNumber.required", num => num.nonEmpty)
      .verifying("error.phoneNumber.invalidLength", num => if (num.nonEmpty) validPhoneNumberLength(num) else true)
      .verifying(
        "error.phoneNumber.invalidFormat",
        num => if (num.nonEmpty && validPhoneNumberLength(num)) num.matches(phoneNumberRegex) else true)
  }

  lazy val individualPersonalDetailsForm: Form[IndividualPersonalDetails] = Form(
    mapping(
      keys.address      -> addressMapping,
      keys.email        -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
      keys.phone        -> validatePhoneNumber,
      keys.mobilePhone  -> validatePhoneNumber,
      keys.tradingName  -> optional(text(maxLength = 45))

    )(IndividualPersonalDetails.apply)(IndividualPersonalDetails.unapply)
  )
  implicit val format = Json.format[IndividualPersonalDetails]

}





