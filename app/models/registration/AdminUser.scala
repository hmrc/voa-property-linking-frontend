/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{Address, IVDetails, IndividualAccountSubmission, IndividualDetails, email => _}
import play.api.data.Forms._
import play.api.data.validation.{Constraints, _}
import play.api.data.{Form, Mapping}
import play.api.libs.json.{Format, JsObject, JsResult, JsValue}
import uk.gov.hmrc.domain.Nino
import utils.EmailAddressValidation
import views.helpers.Errors

trait AdminUser {

  val firstName: String
  val lastName: String
  val address: Address
  val dob: LocalDate
  val nino: Nino
  val phone: String
  val email: String
  val confirmedEmail: String

  def toIndividualAccountSubmission(trustId: String)(user: UserDetails)(id: Long)(organisationId: Option[Long]) = IndividualAccountSubmission(
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

  lazy val individual = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.address -> addressMapping,
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino,
    keys.phone -> nonEmptyText(maxLength = 15),
    keys.mobilePhone -> nonEmptyText(maxLength = 15),
    keys.email -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
    keys.confirmedEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.tradingName -> optional(text(maxLength = 45))
  )(IndividualUserAccountDetails.apply)(IndividualUserAccountDetails.unapply))

  lazy val organisation = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.companyName -> nonEmptyText(maxLength = 45),
    keys.address -> addressMapping,
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino,
    keys.phone -> nonEmptyText.verifying("Maximum length is 15", _.length <= 15),
    keys.email -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
    keys.confirmedBusinessEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.isAgent -> mandatoryBoolean
  )(AdminOrganisationAccountDetails.apply)(AdminOrganisationAccountDetails.unapply))

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) = {
    Nino(nino.toUpperCase.replaceAll(" ", ""))
  }

  implicit val enrolmentUserFormat: Format[AdminUser] = new Format[AdminUser] {
    override def reads(json: JsValue): JsResult[AdminUser] = {
      AdminOrganisationAccountDetails.format.reads(json).orElse(IndividualUserAccountDetails.format.reads(json))
    }

    override def writes(o: AdminUser): JsObject = o match {
      case organisation: AdminOrganisationAccountDetails => AdminOrganisationAccountDetails.format.writes(organisation)
      case individual: IndividualUserAccountDetails => IndividualUserAccountDetails.format.writes(individual)
    }
  }
}
