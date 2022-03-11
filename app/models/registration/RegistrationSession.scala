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
import models.registration.AdminUser.{individual, phoneNumberRegex}
import models.{Address, IVDetails, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.libs.json._
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
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
                                 isAgent:        Option[Boolean] = None,
                                 companyName:    Option[String] = None
                               ) {

  def toGroupDetailsIndividual = GroupAccountDetails(
    companyName = tradingName.getOrElse(truncateCompanyName(s"$firstName $lastName")),
    address = address.getOrElse(Address(addressUnitId = None, line1 = "", line2 = "", line3 = "", line4 = "", postcode = "")),
    email = email.getOrElse(""),
    confirmedEmail = email.getOrElse(""),
    phone = phone.getOrElse(""),
    isAgent = false
  )

  def toGroupDetailsOrganisation = GroupAccountDetails(
    companyName = companyName.getOrElse(""),
    address = address.getOrElse(Address(addressUnitId = None, line1 = "", line2 = "", line3 = "", line4 = "", postcode = "")),
    email = email.getOrElse(""),
    confirmedEmail = email.getOrElse(""),
    phone = phone.getOrElse(""),
    isAgent = isAgent.getOrElse(false)
  )


  //TODO RENAME
  def toAccountSubmission(trustId: Option[String])(user: UserDetails)(id: Long)(
    organisationId: Option[Long]) = IndividualAccountSubmission(
    externalId = user.externalId,
    trustId = trustId,
    organisationId = organisationId,
    details = IndividualDetails(firstName, lastName, email.getOrElse(""), phone.getOrElse(""), Some(mobilePhone.getOrElse("")), id)
  )
  private def truncateCompanyName(companyName: String): String =
    companyName.take(45).toString

}

object RegistrationSession {

  implicit val format = Json.format[RegistrationSession]

  def apply(individualName: IndividualName): RegistrationSession = {
    RegistrationSession(
      firstName = individualName.firstName,
      lastName = individualName.lastName
    )
  }

  def apply(registrationSession: RegistrationSession, individualPersonalDetails: IndividualPersonalDetails): RegistrationSession = {
    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      address = Some(individualPersonalDetails.address),
      email = Some(individualPersonalDetails.email),
      phone = Some(individualPersonalDetails.phone),
      mobilePhone = Some(individualPersonalDetails.mobilePhone),
      tradingName = Some(individualPersonalDetails.tradingName.getOrElse(""))
    )
  }

  def apply(registrationSession: RegistrationSession,
            individualDateOfBirth: IndividualDateOfBirth): RegistrationSession = {

    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      address = registrationSession.address,
      email = registrationSession.email,
      phone = registrationSession.phone,
      mobilePhone = registrationSession.mobilePhone,
      tradingName = registrationSession.tradingName,
      dob = Some(individualDateOfBirth.dob)
    )
  }

  def apply(registrationSession: RegistrationSession, individualNino: IndividualNino): RegistrationSession = {

    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      address = registrationSession.address,
      email = registrationSession.email,
      phone = registrationSession.phone,
      mobilePhone = registrationSession.mobilePhone,
      tradingName = registrationSession.tradingName,
      dob = registrationSession.dob,
      nino = Some(individualNino.nino)
    )
  }

  def apply(organisationName: OrganisationName): RegistrationSession = {
    RegistrationSession(
      firstName = organisationName.firstName,
      lastName = organisationName.lastName
    )
  }

  def apply(registrationSession: RegistrationSession, organisationIsAgent: OrganisationIsAgent): RegistrationSession = {
    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      isAgent = Some(organisationIsAgent.isAgent)
    )
  }

  def apply(registrationSession: RegistrationSession, organisationDetails: OrganisationDetails): RegistrationSession = {

    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      isAgent = registrationSession.isAgent,
      companyName = Some(organisationDetails.companyName),
      address = Some(organisationDetails.address),
      phone = Some(organisationDetails.phone),
      email = Some(organisationDetails.email)
    )
  }

  def apply(registrationSession: RegistrationSession, organisationDateOfBirth: OrganisationDateOfBirth): RegistrationSession = {

    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      isAgent = registrationSession.isAgent,
      companyName = registrationSession.companyName,
      address = registrationSession.address,
      phone = registrationSession.phone,
      email = registrationSession.email,
      dob = Some(organisationDateOfBirth.dob)
    )
  }

  def apply(registrationSession: RegistrationSession, organisationNino: OrganisationNino): RegistrationSession = {

    RegistrationSession(
      firstName = registrationSession.firstName,
      lastName = registrationSession.lastName,
      isAgent = registrationSession.isAgent,
      companyName = registrationSession.companyName,
      address = registrationSession.address,
      phone = registrationSession.phone,
      email = registrationSession.email,
      dob = registrationSession.dob,
      nino = Some(organisationNino.nino)
    )
  }
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

case class IndividualDateOfBirth(dob: LocalDate)

object IndividualDateOfBirth{
  lazy val individualDateOfBirthForm: Form[IndividualDateOfBirth] = Form(
    mapping(
      keys.dateOfBirth -> dmyPastDate
    )(IndividualDateOfBirth.apply)(IndividualDateOfBirth.unapply)
  )
  implicit val format = Json.format[IndividualDateOfBirth]

}

case class IndividualNino(nino: Nino)

object IndividualNino{
  lazy val individualNinoForm: Form[IndividualNino] = Form(
    mapping(
      keys.nino -> nino
    )(IndividualNino.apply)(IndividualNino.unapply)
  )
  implicit val format = Json.format[IndividualNino]

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _                                => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) =
    Nino(nino.toUpperCase.replaceAll(" ", ""))
}


case class OrganisationName(firstName: String, lastName: String)


object OrganisationName{
  lazy val organisationNameForm: Form[OrganisationName] = Form(
    mapping(
      keys.firstName -> nonEmptyText,
      keys.lastName  -> nonEmptyText
    )(OrganisationName.apply)(OrganisationName.unapply)
  )
  implicit val format = Json.format[OrganisationName]

}

case class OrganisationIsAgent(isAgent: Boolean)

object OrganisationIsAgent{
  lazy val organisationIsAgentForm: Form[OrganisationIsAgent] = Form(
    mapping(
      keys.isAgent                -> mandatoryBoolean,
    )(OrganisationIsAgent.apply)(OrganisationIsAgent.unapply)
  )
  implicit val format = Json.format[OrganisationIsAgent]

}

case class OrganisationDetails(companyName: String, address: Address, phone: String, email: String)

object OrganisationDetails{
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

  lazy val organisationDetailsForm: Form[OrganisationDetails] = Form(
    mapping(
      keys.companyName            -> nonEmptyText(maxLength = 45),
      keys.address                -> addressMapping,
      keys.phone                  -> validatePhoneNumber,
      keys.email                  -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_))
    )(OrganisationDetails.apply)(OrganisationDetails.unapply)
  )
  implicit val format = Json.format[OrganisationDetails]

}

case class OrganisationDateOfBirth(dob: LocalDate)

object OrganisationDateOfBirth{
  lazy val organisationDateOfBirthForm: Form[OrganisationDateOfBirth] = Form(
    mapping(
      keys.dateOfBirth -> dmyPastDate
    )(OrganisationDateOfBirth.apply)(OrganisationDateOfBirth.unapply)
  )
  implicit val format = Json.format[OrganisationDateOfBirth]

}

case class OrganisationNino(nino: Nino)

object OrganisationNino{

  lazy val organisationNinoForm: Form[OrganisationNino] = Form(
    mapping(
      keys.nino -> nino
    )(OrganisationNino.apply)(OrganisationNino.unapply)
  )
  implicit val format = Json.format[OrganisationNino]

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _                                => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) =
    Nino(nino.toUpperCase.replaceAll(" ", ""))
}

