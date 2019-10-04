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

import actions.AuthenticatedAction
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import form.Mappings._
import javax.inject.Named
import models.RatesBillType
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import repositories.SessionRepo
import services.BusinessRatesAttachmentService
import session.{LinkingSessionRequest, WithLinkingSession}
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler
import views.html.propertyLinking.declaration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Declaration @Inject()(
                             val errorHandler: CustomErrorHandler,
                             propertyLinks: PropertyLinkConnector,
                            @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                            businessRatesAttachmentService: BusinessRatesAttachmentService,
                            authenticatedAction: AuthenticatedAction,
                            withLinkingSession: WithLinkingSession
                           )(implicit executionContext: ExecutionContext, val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController {

  def show(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession) { implicit request =>
    val isRatesBillEvidence = request.ses.evidenceType.contains(RatesBillType)
    Ok(declaration(DeclarationVM(form), isRatesBillEvidence))
  }

  def submit(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    form.bindFromRequest().fold(
      errors => {
        val isRatesBillEvidence = request.ses.evidenceType.contains(RatesBillType)
        Future.successful(BadRequest(declaration(DeclarationVM(formWithNoDeclaration), isRatesBillEvidence)))
      },
      success =>
        submitLinkingRequest().map( x => Redirect (routes.Declaration.confirmation())))
  }


  def confirmation: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    sessionRepository.remove() map { _ =>
      Ok(views.html.linkingRequestSubmitted(RequestSubmittedVM(request.ses.address, request.ses.submissionId)))
    }
  }

  private def submitLinkingRequest()(implicit request: LinkingSessionRequest[_]): Future[Unit] = {
    for {
      _ <- businessRatesAttachmentService.submitFiles(request.ses.submissionId, request.ses.uploadEvidenceData.attachments)
      _ <- propertyLinks.createPropertyLink()
    } yield ()
  }

  lazy val form = Form(Forms.single("declaration" -> mandatoryBoolean))
  lazy val formWithNoDeclaration = form.withError(FormError("declaration", "declaration.required"))
}

case class DeclarationVM(form: Form[_])

case class RequestSubmittedVM(address: String, refId: String)