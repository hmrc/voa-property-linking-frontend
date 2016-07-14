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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Action
import session.{LinkingSession, WithLinkingSession}
import views.helpers.Errors

object SelfCertification extends PropertyLinkingController {

  def show() = WithLinkingSession { implicit request =>
    Ok(views.html.selfCertification.show(SelfCertifyVM(selfCertifyForm, request.ses)))
  }

  def submit() = WithLinkingSession { implicit request =>
    selfCertifyForm.bindFromRequest().fold(
      errors => BadRequest(views.html.selfCertification.show(SelfCertifyVM(errors, request.ses))),
      conf => Redirect(routes.SelfCertification.linkAuthorised())
    )
  }

  def linkAuthorised() = Action { implicit request =>
    Ok(views.html.selfCertification.linkAuthorised())
  }

  lazy val selfCertifyForm = Form(mapping(
    "iAgree" -> boolean.verifying(Errors.mustAgreeToSelfCert, _ == true)
  )(ConfirmSelfCertification.apply)(ConfirmSelfCertification.unapply))
}

case class ConfirmSelfCertification(iAgree: Boolean)

case class SelfCertifyVM(form: Form[_], session: LinkingSession)
