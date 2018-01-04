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

package controllers

import javax.inject.{Inject, Named}

import auth.GGAction
import connectors.{IndividualAccounts, VPLAuthConnector}
import form.Mappings._
import form.TextMatching
import models.PersonalDetails
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Mapping}
import play.api.mvc.Request
import repositories.SessionRepo
import uk.gov.hmrc.domain.Nino
import views.helpers.Errors
import uk.gov.hmrc.http.SessionKeys

class CreateIndividualAccount @Inject() (ggAction: GGAction,
                                         auth: VPLAuthConnector,
                                         individuals: IndividualAccounts,
                                          @Named ("personSession") val personalDetailsSessionRepo: SessionRepo)
  extends PropertyLinkingController {

  def show = ggAction.async { ctx => implicit request =>
    for {
      externalId <- auth.getExternalId(ctx)
      person <- individuals.withExternalId(externalId)
    } yield {
      person match {
        case Some(_) => Redirect(routes.Dashboard.home)
        case None => showForm
      }
    }
  }

  private def showForm(implicit request: Request[_]) = {
    Ok(views.html.createAccount.individual(CreateIndividualAccount.form))
      .addingToSession( //because SIV wipes the session
        "bearerToken" -> request.session.get(SessionKeys.authToken).getOrElse(""),
        "oldSessionId" -> request.session.get(SessionKeys.sessionId).getOrElse("")
      )
  }

  def submit = ggAction.async { _ => implicit request =>
    CreateIndividualAccount.form.bindFromRequest().fold(
      errors => BadRequest(views.html.createAccount.individual(errors)),
      formData => personalDetailsSessionRepo.saveOrUpdate(formData) map { _ =>
        Redirect(routes.IdentityVerification.startIv)
      }
    )
  }

  implicit def vm(form: Form[_]): CreateIndividualAccountVM = CreateIndividualAccountVM(form)
}

object CreateIndividualAccount{

  lazy val nino: Mapping[Nino] = text.verifying(validNino).transform(toNino, _.nino)

  lazy val validNino: Constraint[String] = Constraint {
    case s if Nino.isValid(s.toUpperCase) => Valid
    case _ => Invalid(ValidationError("error.nino.invalid"))
  }
  lazy val form = Form(mapping(
    keys.firstName -> nonEmptyText(maxLength = 100),
    keys.lastName -> nonEmptyText(maxLength = 100),
    keys.dateOfBirth -> dmyPastDate,
    keys.nino -> nino,
    keys.email -> email.verifying(Constraints.maxLength(150)),
    keys.confirmedEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.phone1 -> nonEmptyText(maxLength = 15),
    keys.phone2 -> optional(text(maxLength = 15)),
    keys.address -> addressMapping
  )(PersonalDetails.apply)(PersonalDetails.unapply))

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
  private def toNino(nino: String) = {
    Nino(nino.toUpperCase.replaceAll(" ", ""))
  }

}
case class CreateIndividualAccountVM(form: Form[_])
