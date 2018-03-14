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
import models.{Address, IVDetails, PersonalDetails}
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import play.api.data.validation.Constraints
import uk.gov.hmrc.domain.Nino
import views.helpers.Errors
import auth.VoaAction
import config.ApplicationConfig
import connectors.{IndividualAccounts, VPLAuthConnector}
import form.Mappings._
import form.TextMatching
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Mapping}
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import repositories.SessionRepo
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import views.helpers.Errors

object CreateEnrolmentIndividualAccount {

  lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) = {
    Nino(nino.toUpperCase.replaceAll(" ", ""))
  }

  lazy val keys = new {
    val firstName = "firstName"
    val lastName = "lastName"
    val address = "address"
    val phone = "phone"
    val mobilePhone = "mobilePhone"
    val email = "email"
    val confirmedEmail = "confirmedEmail"
    val tradingName = "tradingName"
    val dateOfBirth = "dob"
    val nino = "nino"
  }

  lazy val form = Form(mapping(
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

}

case class CreateEnrolmentIndividualAccountVM(form: Form[_])

case class EnrolmentIndividualAccountDetails(firstName: String,
                                             lastName: String,
                                             address: Address,
                                             dob: LocalDate,
                                             nino: Nino,
                                             phone: String,
                                             mobilePhone: String,
                                             email: String,
                                             confirmedEmail: String,
                                             tradingName: Option[String]
                                            ) {

  def toIvDetails = IVDetails(firstName, lastName, dob, nino)
}

case class FieldData(firstName: String = "",
                               lastName: String = "",
                               postcode: String = "",
                               email: String = "")

object FieldData {

  def apply(userInfo: UserInfo): FieldData =
    new FieldData(
      firstName = userInfo.firstName.getOrElse(""),
      lastName = userInfo.lastName.getOrElse(""),
      postcode = userInfo.postcode.getOrElse(""),
      email = userInfo.email
    )
}