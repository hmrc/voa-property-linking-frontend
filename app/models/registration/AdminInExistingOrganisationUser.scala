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
import models.Nino
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Mapping}


trait AdminInExistingOrganisationUser {

  val firstName: String
  val lastName: String
  val dob: LocalDate
  val nino: Nino
}

object AdminInExistingOrganisationUser {

  lazy val organisation = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino
  )(AdminInExistingOrganisationAccountDetails.apply)(AdminInExistingOrganisationAccountDetails.unapply))

  private lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  private lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  private def toNino(nino: String) =
    Nino(nino.toUpperCase.replaceAll(" ", ""))

}
