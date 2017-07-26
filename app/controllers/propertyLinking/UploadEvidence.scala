/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.propertyLinking

import javax.inject.{Inject, Named}

import config.Wiring
import connectors.EnvelopeConnector
import connectors.fileUpload.FileUploadConnector
import controllers._
import form.EnumMapping
import models._
import play.api.data.Form
import play.api.data.Forms._
import repositories.SessionRepo
import session.{LinkingSessionRequest, WithLinkingSession}
import views.html.propertyLinking.uploadEvidence

class UploadEvidence @Inject()(override val fileUploader: FileUploadConnector,
                               override val envelopeConnector: EnvelopeConnector,
                               @Named("propertyLinkingSession") override val sessionRepository: SessionRepo,
                               override val withLinkingSession: WithLinkingSession) extends PropertyLinkingController with FileUploadHelpers {

  override val propertyLinks = Wiring().propertyLinkConnector

  def show(errorCode: Option[Int], errorMessage: Option[String]) = withLinkingSession { implicit request =>
    errorCode match {
      case Some(REQUEST_ENTITY_TOO_LARGE) => EntityTooLarge(uploadEvidence(UploadEvidenceVM(fileTooLarge, submissionUrl)))
      case Some(NOT_FOUND) => notFound
      case Some(UNSUPPORTED_MEDIA_TYPE) => UnsupportedMediaType(uploadEvidence(UploadEvidenceVM(invalidFileType, submissionUrl)))
      case _ => Ok(uploadEvidence(UploadEvidenceVM(form, submissionUrl)))
    }
  }

  def noEvidenceUploaded() = withLinkingSession { implicit request =>
    Redirect(propertyLinking.routes.Declaration.show())
  }

  lazy val form = Form(single("evidenceType" -> EnumMapping(EvidenceType)))
  lazy val fileTooLarge = form.withError("evidence[]", "error.fileUpload.tooLarge")
  lazy val invalidFileType = form.withError("evidence[]", "error.fileUpload.invalidFileType")

  private def submissionUrl(implicit request: LinkingSessionRequest[_]) = fileUploadUrl(routes.UploadEvidence.show().url)
}

case class UploadEvidenceVM(form: Form[_], submissionUrl: String)
