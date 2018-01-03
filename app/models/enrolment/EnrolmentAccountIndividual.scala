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

object CreateEnrolmentIndividualAccount {
  lazy val keys = new {
    val firstName = "firstName"
    val lastName = "lastName"
    val address = "address"
    val phone = "phone"
    val mobilePhone = "mobilePhone"
    val email = "email"
    val confirmedEmail = "confirmedEmail"
  }

  lazy val form = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.address -> addressMapping,
    keys.phone -> nonEmptyText(maxLength = 20),
    keys.mobilePhone -> nonEmptyText, //Pretty sure this is meant to be optional.
    keys.email -> email.verifying(Constraints.maxLength(150)),
    keys.confirmedEmail -> TextMatching(keys.email, Errors.emailsMustMatch)
  )(EnrolmentIndividualAccountDetails.apply)(EnrolmentIndividualAccountDetails.unapply))


}

case class CreateEnrolmentIndividualAccountVM(form: Form[_])

case class EnrolmentIndividualAccountDetails(firstName: String,
                                             lastName: String,
                                             address: Address,
                                             phone: String,
                                             mobilePhone: String,
                                             email: String,
                                             confirmedEmail: String)

case class FieldData(firstName: String = "",
                               lastName: String = "",
                               postcode: String = "",
                               email: String = "")

object FieldData {

  def apply(userInfo: UserInfo): FieldData =
    new FieldData(
      firstName = userInfo.firstName,
      lastName = userInfo.lastName,
      postcode = userInfo.postcode,
      email = userInfo.email)

  implicit private def get(opt: Option[String]): String = opt match {
    case Some(x)  => x
    case None     => ""
  } //TODO Just playing around with implicits. will delete.
}