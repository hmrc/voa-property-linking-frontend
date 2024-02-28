/*
 * Copyright 2023 HM Revenue & Customs
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
import actions.propertylinking.{WithLinkingSession, WithSubmittedLinkingSession}
import binders.propertylinks.EvidenceChoices
import binders.propertylinks.EvidenceChoices.NO_LEASE_OR_LICENSE
import cats.data.OptionT
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings._

import javax.inject.Named
import models.propertylinking.requests.PropertyLinkRequest
import models._
import models.attachment.Attachment
import play.api.Logging
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
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
      withSubmittedLinkingSession: WithSubmittedLinkingSession,
      declarationView: views.html.propertyLinking.declaration,
      linkingRequestSubmittedView: views.html.linkingRequestSubmitted
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController with Cats with Logging {

  lazy val form = Form(Forms.single("declaration" -> mandatoryBoolean))
  lazy val formWithNoDeclaration = form.withError(FormError("declaration", "declaration.required"))

  def show: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    sessionRepository
      .saveOrUpdate(request.ses.copy(fromCya = Some(true)))
      .map { _ =>
        Ok(declarationView(DeclarationVM(form, request.ses.address, request.ses.localAuthorityReference)))
      }
  }

  def back: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    sessionRepository
      .saveOrUpdate(request.ses.copy(fromCya = Some(false)))
      .map { _ =>
        val evidenceType: Option[EvidenceType] = request.ses.uploadEvidenceData.fileInfo.map(_.evidenceType)
        val capacityType: Option[CapacityType] = request.ses.propertyRelationship.map(_.capacity)
        println(Console.BLUE + evidenceType + Console.RESET)
        println(Console.BLUE + capacityType + Console.RESET)
        val evidenceChoice: Option[EvidenceChoices.Value] = evidenceType.zip(capacityType).headOption.map {
          case (RatesBillType, Owner | OwnerOccupier | Occupier)        => EvidenceChoices.RATES_BILL
          case (ServiceCharge, Owner | OwnerOccupier | Occupier)        => EvidenceChoices.SERVICE_CHARGE
          case (StampDutyLandTaxForm, Owner | OwnerOccupier | Occupier) => EvidenceChoices.STAMP_DUTY
          case (LandRegistryTitle, Owner | OwnerOccupier | Occupier)    => EvidenceChoices.LAND_REGISTRY
          case (WaterRateDemand, Owner | OwnerOccupier | Occupier)      => EvidenceChoices.WATER_RATE
          case (OtherUtilityBill, Owner | OwnerOccupier | Occupier)     => EvidenceChoices.UTILITY_RATE
          case (Lease, Occupier | Owner | OwnerOccupier | Occupier)     => EvidenceChoices.LEASE
          case (License, Occupier | Owner | OwnerOccupier | Occupier)   => EvidenceChoices.LICENSE
          case (_, Owner | OwnerOccupier | Occupier)                    => EvidenceChoices.OTHER
          case (_, Occupier)                                            => EvidenceChoices.NO_LEASE_OR_LICENSE
        }

        evidenceChoice.fold {
          Redirect(
            if (evidenceType.isEmpty) routes.ChooseEvidenceController.show
            else routes.ClaimPropertyRelationshipController.back
          )
        } { choice =>
//          if (choice == NO_LEASE_OR_LICENSE)
//            Redirect(routes.UploadController.show(choice))
//          else
            Redirect(routes.UploadResultController.show(choice))
        }
      }
  }

  /*
    We should have extra validation here to catch users that get by without supplying on the necessary information.
   */
  def submit: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
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
                      Redirect(routes.ChooseEvidenceController.show)
                  }
              },
              _ => Redirect(routes.DeclarationController.confirmation)
            )

        }
      )
  }

  def confirmation: Action[AnyContent] = authenticatedAction.andThen(withSubmittedLinkingSession).async {
    implicit request =>
      sessionRepository
        .saveOrUpdate(request.ses.copy(isSubmitted = Some(true)))
        .map { _ =>
          Ok(
            linkingRequestSubmittedView(
              RequestSubmittedVM(
                request.ses.address,
                request.ses.submissionId,
                request.ses.clientDetails,
                request.ses.localAuthorityReference)))
        }
  }
}

case class DeclarationVM(form: Form[_], address: String, localAuthorityReference: String)

case class RequestSubmittedVM(
      address: String,
      refId: String,
      clientDetails: Option[ClientDetails] = None,
      localAuthorityReference: String)
