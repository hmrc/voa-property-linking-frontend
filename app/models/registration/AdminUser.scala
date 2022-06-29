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

import java.time.LocalDate

import form.Mappings._
import form.TextMatching
import models.domain._
import models.{Address, IVDetails, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Mapping}
import play.api.libs.json._
import utils.EmailAddressValidation
import utils.PhoneNumberValidation.validatePhoneNumber
import views.helpers.Errors

sealed trait AdminUser extends User {

  val firstName: String
  val lastName: String
  val address: Address
  val dob: LocalDate
  val nino: Nino
  val phone: String
  val email: String
  val confirmedEmail: String

  def toIndividualAccountSubmission(trustId: Option[String])(user: UserDetails)(id: Long)(
        organisationId: Option[Long]) =
    IndividualAccountSubmission(
      externalId = user.externalId,
      trustId = trustId,
      organisationId = organisationId,
      details = IndividualDetails(firstName, lastName, email, phone, None, id)
    )

  def toIvDetails = IVDetails(
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = Some(dob),
    nino = Some(nino)
  )
}

object AdminUser {

  lazy val individual: Form[IndividualUserAccountDetails] = Form(
    mapping(
      keys.firstName       -> nonEmptyText,
      keys.lastName        -> nonEmptyText,
      keys.address         -> addressMapping,
      keys.dateOfBirth     -> dmyPastDate,
      keys.nino            -> nino,
      keys.phone           -> validatePhoneNumber,
      keys.mobilePhone     -> validatePhoneNumber, //FIXME mobile phone regex
      keys.email           -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
      keys.confirmedEmail  -> TextMatching(keys.email, Errors.emailsMustMatch),
      keys.tradingName     -> optional(text(maxLength = 45)),
      keys.selectedAddress -> optional(text)
    )(IndividualUserAccountDetails.apply)(IndividualUserAccountDetails.unapply))

  lazy val organisation = Form(
    mapping(
      keys.firstName              -> nonEmptyText,
      keys.lastName               -> nonEmptyText,
      keys.companyName            -> nonEmptyText(maxLength = 45),
      keys.address                -> addressMapping,
      keys.dateOfBirth            -> dmyPastDate,
      keys.nino                   -> nino,
      keys.phone                  -> validatePhoneNumber,
      keys.email                  -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
      keys.confirmedBusinessEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
      keys.isAgent                -> mandatoryBoolean,
      keys.selectedAddress        -> optional(text)
    )(AdminOrganisationAccountDetails.apply)(AdminOrganisationAccountDetails.unapply))

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _                                => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) =
    Nino(nino.toUpperCase.replaceAll(" ", ""))

  implicit val enrolmentUserFormat: Format[AdminUser] = new Format[AdminUser] {
    override def reads(json: JsValue): JsResult[AdminUser] =
      AdminOrganisationAccountDetails.format.reads(json).orElse(IndividualUserAccountDetails.format.reads(json))

    override def writes(o: AdminUser): JsObject = o match {
      case organisation: AdminOrganisationAccountDetails => AdminOrganisationAccountDetails.format.writes(organisation)
      case individual: IndividualUserAccountDetails      => IndividualUserAccountDetails.format.writes(individual)
    }
  }
}

trait AdminInExistingOrganisationUser extends AdminUser {

  val firstName: String
  val lastName: String
  val dob: LocalDate
  val nino: Nino
}

object AdminInExistingOrganisationUser {

  lazy val organisation = Form(
    mapping(
      keys.firstName   -> nonEmptyText,
      keys.lastName    -> nonEmptyText,
      keys.dateOfBirth -> dmyPastDate,
      keys.nino        -> nino
    )(AdminInExistingOrganisationAccountDetails.apply)(AdminInExistingOrganisationAccountDetails.unapply))

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _                                => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) =
    Nino(nino.toUpperCase.replaceAll(" ", ""))

}

case class AdminInExistingOrganisationAccountDetails(firstName: String, lastName: String, dob: LocalDate, nino: Nino)
    extends AdminInExistingOrganisationUser {

  override val address = Address(None, "", "", "", "", "")
  override val phone = ""
  override val email = ""
  override val confirmedEmail = ""

  def toAdminOrganisationAccountDetails(fieldData: FieldData) = AdminOrganisationAccountDetails(
    companyName = fieldData.businessName,
    address = fieldData.businessAddress,
    email = fieldData.email,
    confirmedEmail = fieldData.email,
    phone = fieldData.businessPhoneNumber,
    isAgent = fieldData.isAgent,
    firstName = firstName,
    lastName = lastName,
    dob = dob,
    nino = nino
  )

}

object AdminInExistingOrganisationAccountDetails {
  implicit val format = Json.format[AdminInExistingOrganisationAccountDetails]
  implicit val formatGroupAccountDetails = Json.format[GroupAccountDetails]
}

case class AdminOrganisationAccountDetails(
      firstName: String,
      lastName: String,
      companyName: String,
      address: Address,
      dob: LocalDate,
      nino: Nino,
      phone: String,
      email: String,
      confirmedEmail: String,
      isAgent: Boolean,
      selectedAddress: Option[String] = None)
    extends AdminUser {

  def toGroupDetails = GroupAccountDetails(
    companyName = companyName,
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = isAgent
  )
}

object AdminOrganisationAccountDetails {
  implicit val format = Json.format[AdminOrganisationAccountDetails]
}

case class GroupAccountDetails(
      companyName: String,
      address: Address,
      email: String,
      confirmedEmail: String,
      phone: String,
      isAgent: Boolean)

case class IndividualUserAccountDetails(
      firstName: String,
      lastName: String,
      address: Address,
      dob: LocalDate,
      nino: Nino,
      phone: String,
      mobilePhone: String,
      email: String,
      confirmedEmail: String,
      tradingName: Option[String],
      selectedAddress: Option[String] = None)
    extends AdminUser {

  def toGroupDetails = GroupAccountDetails(
    companyName = tradingName.getOrElse(truncateCompanyName(s"$firstName $lastName")),
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = false
  )

  override def toIndividualAccountSubmission(trustId: Option[String])(user: UserDetails)(id: Long)(
        organisationId: Option[Long]) = IndividualAccountSubmission(
    externalId = user.externalId,
    trustId = trustId,
    organisationId = organisationId,
    details = IndividualDetails(firstName, lastName, email, phone, Some(mobilePhone), id)
  )

  private def truncateCompanyName(companyName: String): String =
    companyName.take(45).toString
}

object IndividualUserAccountDetails {
  implicit val format = Json.format[IndividualUserAccountDetails]
}
