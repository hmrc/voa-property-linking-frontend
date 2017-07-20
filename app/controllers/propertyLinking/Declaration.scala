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

import javax.inject.Named

import com.google.inject.{Inject, Singleton}
import config.Wiring
import connectors.EnvelopeConnector
import connectors.fileUpload.{FileMetadata, FileUploadConnector}
import controllers.PropertyLinkingController
import form.Mappings._
import models.NoEvidenceFlag
import play.api.data.{Form, FormError, Forms}
import repositories.SessionRepo
import session.{LinkingSessionRequest, WithLinkingSession}
import views.html.propertyLinking.declaration

@Singleton
class Declaration @Inject()(envelopes: EnvelopeConnector,
                            fileUploads: FileUploadConnector,
                            @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                            withLinkingSession: WithLinkingSession)
  extends PropertyLinkingController {

  val propertyLinks = Wiring().propertyLinkConnector

  def show = withLinkingSession { implicit request =>
    Ok(declaration(DeclarationVM(form)))
  }

  def submit = withLinkingSession { implicit request =>
    form.bindFromRequest().value match {
      case Some(true) => fileUploads.getFileMetadata(request.ses.envelopeId) flatMap {
        case data@FileMetadata(_, Some(info)) => submitLinkingRequest(data) map { _ => Redirect(routes.Declaration.confirmation()) }
        case data@FileMetadata(NoEvidenceFlag, _) => submitLinkingRequest(data) map { _ => Redirect(routes.Declaration.noEvidence()) }
      }
      case _ => BadRequest(declaration(DeclarationVM(formWithNoDeclaration)))
    }
  }

  def confirmation = withLinkingSession { implicit request =>
    sessionRepository.remove() map { _ =>
      Ok(views.html.linkingRequestSubmitted(RequestSubmittedVM(request.ses.address, request.ses.submissionId)))
    }
  }

  def noEvidence = withLinkingSession { implicit request =>
    sessionRepository.remove() map { _ => Ok(views.html.propertyLinking.noEvidenceUploaded(RequestSubmittedVM(request.ses.address, request.ses.submissionId))) }
  }

  private def submitLinkingRequest(info: FileMetadata)(implicit request: LinkingSessionRequest[_]) = {
    for {
      _ <- propertyLinks.linkToProperty(info)
      _ <- envelopes.closeEnvelope(request.ses.envelopeId)
    } yield ()
  }

  lazy val form = Form(Forms.single("declaration" -> mandatoryBoolean))
  lazy val formWithNoDeclaration = form.withError(FormError("declaration", "declaration.required"))
}

case class DeclarationVM(form: Form[_])

case class RequestSubmittedVM(address: String, refId: String)