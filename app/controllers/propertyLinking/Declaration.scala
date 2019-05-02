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
import play.api.mvc.{Action, AnyContent}
import repositories.SessionRepo
import session.{LinkingSessionRequest, WithLinkingSession}
import views.html.propertyLinking.declaration

import scala.concurrent.Future

@Singleton
class Declaration @Inject()(envelopes: EnvelopeConnector,
                            fileUploads: FileUploadConnector,
                            propertyLinks: PropertyLinkConnector,
                            @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                            withLinkingSession: WithLinkingSession)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController {

  def show(noEvidenceFlag: Option[Boolean] = None): Action[AnyContent] = withLinkingSession { implicit request =>
    Future.successful(Ok(declaration(DeclarationVM(form), noEvidenceFlag)))
  }

  def submit(noEvidenceFlag: Option[Boolean] = None): Action[AnyContent] = withLinkingSession { implicit request =>
    if (config.fileUploadEnabled) {
      form.bindFromRequest().value match {
        case Some(true) => fileUploads.getFileMetadata(request.ses.envelopeId) flatMap {
          case data@FileMetadata(_, Some(info)) => submitLinkingRequest(data) map { _ => Redirect(routes.Declaration.confirmation()) }
          case data@FileMetadata(NoEvidenceFlag, _) => submitLinkingRequest(data) map { _ => Redirect(routes.Declaration.noEvidence()) }
        }
        case _          => Future.successful(BadRequest(declaration(DeclarationVM(formWithNoDeclaration))))
      }
    } else {
      form.bindFromRequest().value match {
        case Some(true) => noEvidenceFlag match {
          case Some(true) => submitLinkingRequest(FileMetadata(NoEvidenceFlag, None)) map { _ => Redirect(routes.Declaration.noEvidence()) }
          case _          => submitLinkingRequest(FileMetadata(RatesBillFlag, Some(FileInfo("stubbedFile", RatesBillType)))) map { _ => Redirect(routes.Declaration.confirmation()) }
        }
        case _          => Future.successful(BadRequest(declaration(DeclarationVM(formWithNoDeclaration))))
      }
    }
  }

  def confirmation: Action[AnyContent] = withLinkingSession { implicit request =>
    sessionRepository.remove() map { _ =>
      Ok(views.html.linkingRequestSubmitted(RequestSubmittedVM(request.ses.address, request.ses.submissionId)))
    }
  }

  def noEvidence: Action[AnyContent] = withLinkingSession { implicit request =>
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