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

import config.Wiring
import form.Mappings._
import models.PersonalDetails
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel
import uk.gov.hmrc.play.http.SessionKeys

trait CreateIndividualAccount extends PropertyLinkingController {
  val accounts = Wiring().individualAccountConnector
  val ggAction = Wiring().ggAction
  val keystore = Wiring().sessionCache
  val identityVerification = Wiring().identityVerification

  def show = ggAction { ctx => implicit request =>
    if (ctx.user.confidenceLevel >= ConfidenceLevel.L200) {
      Redirect(routes.CreateGroupAccount.show)
    } else {
      //TODO - temporary fix for PE-2543
      Ok(views.html.createAccount.individual(form))
        .addingToSession(
          "bearerToken" -> request.session.get(SessionKeys.authToken).getOrElse(""),
          "oldSessionId" -> request.session.get(SessionKeys.sessionId).getOrElse("")
        )
    }
  }

  def submit = ggAction.async { _ => implicit request =>
    form.bindFromRequest().fold(
      errors => BadRequest(views.html.createAccount.individual(errors)),
      formData => keystore.cachePersonalDetails(formData) map { _ =>
        Redirect(routes.IdentityVerification.startIv)
      }
    )
  }

  lazy val keys = new {
    val firstName = "fname"
    val lastName = "lname"
    val dateOfBirth = "dob"
    val nino = "nino.nino"
    val email = "email"
    val confirmedEmail = "confirmedEmail"
    val phone1 = "phone1"
    val phone2 = "phone2"
    val address = "address"
  }

  lazy val form = Form(mapping(
    keys.firstName -> nonEmptyText,
    keys.lastName -> nonEmptyText,
    keys.dateOfBirth -> dmyDate,
    keys.nino -> nino,
    keys.email -> email,
    keys.confirmedEmail -> email,
    keys.phone1 -> nonEmptyText,
    keys.phone2 -> optional(text),
    keys.address -> address
  )(PersonalDetails.apply)(PersonalDetails.unapply))

  lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(Nino(_), _.nino)

  lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }

  implicit def vm(form: Form[_]): CreateIndividualAccountVM = CreateIndividualAccountVM(form)
}

object CreateIndividualAccount extends CreateIndividualAccount

case class CreateIndividualAccountVM(form: Form[_])