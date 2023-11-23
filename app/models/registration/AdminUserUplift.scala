/*
 * Copyright 2023 HM Revenue & Customs
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
import form.TextMatching
import models.domain._
import models.{Address, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import utils.EmailAddressValidation
import utils.PhoneNumberValidation.validatePhoneNumber
import views.helpers.Errors

import java.time.LocalDate

sealed trait AdminUserUplift {

  val address: Address
  val phone: String
  val email: String
  val confirmedEmail: String

  def toIndividualAccountSubmissionUplift(trustId: Option[String], firstName: Option[String], lastName: Option[String])(
        user: UserDetails)(id: Long)(organisationId: Option[Long]) =
    IndividualAccountSubmission(
      externalId = user.externalId,
      trustId = trustId,
      organisationId = organisationId,
      details = IndividualDetails(
        firstName.getOrElse(throw new Exception(s"Missing itmp first name")),
        lastName.getOrElse(throw new Exception(s"Missing itmp last name")),
        email,
        phone,
        None,
        id
      )
    )

}

object AdminUserUplift {

  lazy val individual: Form[IndividualUserAccountDetailsUplift] = Form(
    mapping(
      keys.address         -> addressMapping,
      keys.phone           -> validatePhoneNumber,
      keys.mobilePhone     -> validatePhoneNumber, //FIXME mobile phone regex
      keys.email           -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
      keys.confirmedEmail  -> TextMatching(keys.email, Errors.emailsMustMatch),
      keys.tradingName     -> optional(text(maxLength = 45)),
      keys.selectedAddress -> optional(text)
    )(IndividualUserAccountDetailsUplift.apply)(IndividualUserAccountDetailsUplift.unapply))

  lazy val organisation = Form(
    mapping(
      keys.companyName            -> nonEmptyText(maxLength = 45),
      keys.address                -> addressMapping,
      keys.phone                  -> validatePhoneNumber,
      keys.email                  -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
      keys.confirmedBusinessEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
      keys.isAgent                -> mandatoryBoolean,
      keys.selectedAddress        -> optional(text)
    )(AdminOrganisationAccountDetailsUplift.apply)(AdminOrganisationAccountDetailsUplift.unapply))

  implicit val enrolmentUserFormat: Format[AdminUserUplift] = new Format[AdminUserUplift] {
    override def reads(json: JsValue): JsResult[AdminUserUplift] =
      AdminOrganisationAccountDetailsUplift.format
        .reads(json)
        .orElse(IndividualUserAccountDetailsUplift.format.reads(json))

    override def writes(o: AdminUserUplift): JsObject = o match {
      case organisation: AdminOrganisationAccountDetailsUplift =>
        AdminOrganisationAccountDetailsUplift.format.writes(organisation)
      case individual: IndividualUserAccountDetailsUplift =>
        IndividualUserAccountDetailsUplift.format.writes(individual)
    }
  }
}

case class AdminInExistingOrganisationAccountDetailsUplift(
      firstName: String,
      lastName: String,
      dob: LocalDate,
      nino: Nino)
    extends AdminInExistingOrganisationUser {

  override val address = Address(None, "", "", "", "", "")
  override val phone = ""
  override val email = ""
  override val confirmedEmail = ""

  def toAdminOrganisationAccountDetailsUplift(fieldData: FieldData) = AdminOrganisationAccountDetailsUplift(
    companyName = fieldData.businessName,
    address = fieldData.businessAddress,
    email = fieldData.email,
    confirmedEmail = fieldData.email,
    phone = fieldData.businessPhoneNumber,
    isAgent = fieldData.isAgent
  )

}

object AdminInExistingOrganisationAccountDetailsUplift {
  implicit val format = Json.format[AdminInExistingOrganisationAccountDetailsUplift]
  implicit val formatGroupAccountDetails = Json.format[GroupAccountDetails]
}

case class AdminOrganisationAccountDetailsUplift(
      companyName: String,
      address: Address,
      phone: String,
      email: String,
      confirmedEmail: String,
      isAgent: Boolean,
      selectedAddress: Option[String] = None)
    extends AdminUserUplift {

  def toGroupDetails = GroupAccountDetails(
    companyName = companyName,
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = isAgent
  )
}

object AdminOrganisationAccountDetailsUplift {
  implicit val format = Json.format[AdminOrganisationAccountDetailsUplift]
}

case class IndividualUserAccountDetailsUplift(
      address: Address,
      phone: String,
      mobilePhone: String,
      email: String,
      confirmedEmail: String,
      tradingName: Option[String],
      selectedAddress: Option[String] = None)
    extends AdminUserUplift {

  def toGroupDetails(firstName: Option[String], lastName: Option[String]) = GroupAccountDetails(
    companyName = tradingName.getOrElse(truncateCompanyName(s"$firstName $lastName")),
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = false
  )

  override def toIndividualAccountSubmissionUplift(
        trustId: Option[String],
        firstName: Option[String],
        lastName: Option[String])(user: UserDetails)(id: Long)(organisationId: Option[Long]) =
    IndividualAccountSubmission(
      externalId = user.externalId,
      trustId = trustId,
      organisationId = organisationId,
      details = IndividualDetails(
        firstName.getOrElse(throw new Exception(s"Missing itmp first name")),
        lastName.getOrElse(throw new Exception(s"Missing itmp last name")),
        email,
        phone,
        Some(mobilePhone),
        id
      )
    )

  private def truncateCompanyName(companyName: String): String =
    companyName.take(45).toString
}

object IndividualUserAccountDetailsUplift {
  implicit val format = Json.format[IndividualUserAccountDetailsUplift]
}
