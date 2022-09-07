/*
 * Copyright 2022 HM Revenue & Customs
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
import actions.propertylinking.WithLinkingSession
import binders.propertylinks.EvidenceChoices
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings._

import javax.inject.Named
import models.propertylinking.requests.PropertyLinkRequest
import models._
import play.api.Logging
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.propertylinking.PropertyLinkingService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.hmrc.propertylinking.exceptions.attachments.{MissingRequiredNumberOfFiles, NotAllFilesReadyToUpload}
import utils.Cats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeclarationController @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinkService: PropertyLinkingService,
      @Named("propertyLinkingSession") sessionRepository: SessionRepo,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      declarationView: views.html.propertyLinking.declaration,
      linkingRequestSubmittedView: views.html.linkingRequestSubmitted
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController with Cats with Logging {

  def show(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    sessionRepository
      .saveOrUpdate(request.ses.copy(fromCya = Some(true)))
      .map { _ =>
        Ok(declarationView(DeclarationVM(form, request.ses.address, request.ses.localAuthorityReference)))
      }
  }

  def back(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    sessionRepository
      .saveOrUpdate(request.ses.copy(fromCya = Some(false)))
      .map { _ =>
        Redirect(
          routes.UploadController.show(
            if (request.ses.uploadEvidenceData.linkBasis == RatesBillFlag)
              EvidenceChoices.RATES_BILL
            else
              EvidenceChoices.OTHER))
      }
  }

  /*
    We should have extra validation here to catch users that get by without supplying on the necessary information.
   */
  def submit(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        _ => {
          Future.successful(
            BadRequest(
              declarationView(
                DeclarationVM(
                  formWithNoDeclaration,
                  request.ses.address,
                  request.ses.localAuthorityReference
                ))))
        },
        _ =>
          for {
            submitResult <- propertyLinkService
                             .submit(
                               PropertyLinkRequest(request.ses, request.organisationId),
                               request.ses.clientDetails.map(_.organisationId))
                             .value
          } yield {
            submitResult.fold(
              {
                case NotAllFilesReadyToUpload =>
                  logger.warn(
                    s"Not all files are ready for upload on submission for ${request.ses.submissionId}, redirecting back to declaration page")
                  BadRequest(
                    declarationView(
                      DeclarationVM(
                        form.fill(true).withError("declaration", "declaration.file.receipt"),
                        request.ses.address,
                        request.ses.localAuthorityReference)
                    ))
                case MissingRequiredNumberOfFiles =>
                  logger.warn(
                    s"Missing at least 1 evidence uploaded for ${request.ses.submissionId}, redirecting back to upload screens.")
                  request.ses.evidenceType match {
                    case Some(RatesBillType) =>
                      Redirect(routes.UploadController.show(EvidenceChoices.RATES_BILL))
                    case Some(_) =>
                      Redirect(routes.UploadController.show(EvidenceChoices.OTHER))
                    case None =>
                      Redirect(routes.ChooseEvidenceController.show())
                  }
              },
              _ => Redirect(routes.DeclarationController.confirmation)
            )

        }
      )
  }

  def confirmation: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    sessionRepository.remove().map { _ =>
      Ok(
        linkingRequestSubmittedView(
          RequestSubmittedVM(
            request.ses.address,
            request.ses.submissionId,
            request.ses.clientDetails,
            request.ses.localAuthorityReference)))
    }
  }

  lazy val form = Form(Forms.single("declaration" -> mandatoryBoolean))
  lazy val formWithNoDeclaration = form.withError(FormError("declaration", "declaration.required"))
}

case class DeclarationVM(form: Form[_], address: String, localAuthorityReference: String)

case class RequestSubmittedVM(
      address: String,
      refId: String,
      clientDetails: Option[ClientDetails] = None,
      localAuthorityReference: String)
