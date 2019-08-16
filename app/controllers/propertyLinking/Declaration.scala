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

package controllers.propertyLinking

import javax.inject.Named

import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import connectors.EnvelopeConnector
import connectors.fileUpload.{FileMetadata, FileUploadConnector}
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import form.Mappings._
import models._
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.MessagesApi
import repositories.SessionRepo
import services.BusinessRatesAttachmentService
import session.{LinkingSessionRequest, WithLinkingSession}
import views.html.propertyLinking.declaration

import scala.concurrent.Future

@Singleton
class Declaration @Inject()(envelopes: EnvelopeConnector,
                            fileUploads: FileUploadConnector,
                            propertyLinks: PropertyLinkConnector,
                            @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                            businessRatesAttachmentService: BusinessRatesAttachmentService,
                            withLinkingSession: WithLinkingSession)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController {

  def show(noEvidenceFlag: Option[Boolean] = None) = withLinkingSession { implicit request =>
    Ok(declaration(DeclarationVM(form), noEvidenceFlag))
  }

  def submit(noEvidenceFlag: Option[Boolean] = None) = withLinkingSession { implicit request =>
      form.bindFromRequest().value match {
        submitLinkingRequest()
        Redirect(routes.Declaration.confirmation())
        }
        case _ => BadRequest(declaration(DeclarationVM(formWithNoDeclaration)))
  }


  def confirmation = withLinkingSession { implicit request =>
    sessionRepository.remove() map { _ =>
      Ok(views.html.linkingRequestSubmitted(RequestSubmittedVM(request.ses.address, request.ses.submissionId)))
    }
  }

  def noEvidence = withLinkingSession { implicit request =>
    sessionRepository.remove() map { _ => Ok(views.html.propertyLinking.noEvidenceUploaded(RequestSubmittedVM(request.ses.address, request.ses.submissionId))) }
  }

  private def submitLinkingRequest()(implicit request: LinkingSessionRequest[_]) = {
    for {
      - <- businessRatesAttachmentService.submitFiles(request.ses.submissionId, request.ses.uploadEvidenceData.attachments)
      _ <- propertyLinks.linkToProperty()
      _ <- envelopes.closeEnvelope(request.ses.envelopeId)
    } yield ()
  }

  lazy val form = Form(Forms.single("declaration" -> mandatoryBoolean))
  lazy val formWithNoDeclaration = form.withError(FormError("declaration", "declaration.required"))
}

case class DeclarationVM(form: Form[_])

case class RequestSubmittedVM(address: String, refId: String)