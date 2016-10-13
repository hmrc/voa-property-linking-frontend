/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import auth.GGAction
import config.Wiring
import form.Mappings._
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.domain.Nino

trait CreateIndividualAccount extends PropertyLinkingController {
  val accounts = Wiring().accountConnector
  val ggAction = Wiring().ggAction

  def show = ggAction { _ => implicit request =>
    Ok(views.html.createAccount.individual(form))
  }

  def submit = ggAction { _ => implicit request =>
    form.bindFromRequest().fold(
      errors => BadRequest(views.html.createAccount.individual(errors)),
      formData => Redirect(routes.IdentityVerification.show)
    )
  }

  lazy val keys = new {
    val firstName = "fname"
    val lastName = "lname"
    val dateOfBirth = "dateOfBirth"
    val nino = "nino.nino"
  }

  lazy val form = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.dateOfBirth -> dmyDate,
    keys.nino -> nino
  )(IndividualAccount.apply)(IndividualAccount.unapply))

  lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(Nino(_), _.nino)

  lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  implicit def vm(form: Form[_]): CreateIndividualAccountVM = CreateIndividualAccountVM(form)
}

object CreateIndividualAccount extends CreateIndividualAccount

case class IndividualAccount(firstName: String, lastName: String, dateOfBirth: DateTime, nino: Nino)

case class CreateIndividualAccountVM(form: Form[_])
