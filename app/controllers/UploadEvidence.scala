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
import session.WithLinkingSession

object UploadEvidence extends PropertyLinkingController {

  def show() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.show(UploadEvidenceVM(form)))
  }

  def submit() = WithLinkingSession { implicit request =>
    form.bindFromRequest().fold(
      error => BadRequest(views.html.uploadEvidence.show(UploadEvidenceVM(error))),
      uploaded => Redirect(routes.UploadEvidence.evidenceUploaded())
    )
  }

  def evidenceUploaded() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.evidenceUploaded())
  }

  lazy val form = Form(mapping(
    "hasEvidence" -> boolean
  )(UploadedEvidence.apply)(UploadedEvidence.unapply))
}

case class UploadedEvidence(hasEvidence: Boolean)

case class UploadEvidenceVM(form: Form[_])
