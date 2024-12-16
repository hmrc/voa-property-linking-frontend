/*
 * Copyright 2024 HM Revenue & Customs
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
import binders.propertylinks.EvidenceChoices.{EvidenceChoices, OTHER, RATES_BILL}
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings._
import models.{CompleteFileInfo, EvidenceType, LandRegistryTitle, Lease, License, LinkingSession, NoLeaseOrLicense, OccupierEvidenceType, OtherUtilityBill, RatesBillType, ServiceCharge, StampDutyLandTaxForm, UploadEvidenceData, WaterRateDemand}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseEvidenceController @Inject() (
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      businessRatesAttachmentService: BusinessRatesAttachmentsService,
      chooseEvidenceView: views.html.propertyLinking.chooseEvidence,
      chooseOccupierEvidenceView: views.html.propertyLinking.chooseOccupierEvidence
)(implicit
      executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def show: Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      val form = request.ses.propertyRelationship match {
        case Some(relationship) if relationship.isOccupier =>
          request.ses.occupierEvidenceType.fold(OccupierEvidenceType.form)(OccupierEvidenceType.form.fillAndValidate)
        case _ => request.ses.hasRatesBill.fold(ChooseEvidence.form)(ChooseEvidence.form.fillAndValidate)
      }

      for {
        _ <- businessRatesAttachmentService.persistSessionData(request.ses.copy(fromCya = Some(false)))
        backLink = backlink(request.ses)
      } yield request.ses.propertyRelationship match {
        case Some(relationship) if relationship.isOccupier => Ok(chooseOccupierEvidenceView(form, Some(backLink)))
        case _                                             => Ok(chooseEvidenceView(form, Some(backLink)))
      }
    }

  private def backlink(session: LinkingSession): String =
    if (session.earliestStartDate.isAfter(LocalDate.now))
      controllers.propertyLinking.routes.ClaimPropertyRelationshipController.back.url
    //    keeping this because it will need to be re-implemented after VTCCA-5189 is complete
    //    else if (session.fromCya.contains(true))
    //      controllers.propertyLinking.routes.DeclarationController.show().url
    else
      controllers.propertyLinking.routes.ClaimPropertyOccupancyController.showOccupancy.url

  def submit: Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      ChooseEvidence.form
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(chooseEvidenceView(errors, Some(backlink(request.ses))))),
          hasRatesBill => {
            val evidence = if (hasRatesBill) RATES_BILL else OTHER

            request.ses.hasRatesBill match {
              case Some(bool) if bool == hasRatesBill =>
                if (hasRatesBill)
                  request.ses.uploadEvidenceData.fileInfo match {
                    case Some(CompleteFileInfo(_, evidenceType)) =>
                      Future
                        .successful(Redirect(routes.UploadResultController.show(getEvidenceChoice(Some(evidenceType)))))
                    case _ =>
                      Future.successful(Redirect(routes.UploadController.show(evidence)))
                  }
                else Future.successful(Redirect(routes.UploadController.show(evidence)))
              case _ =>
                businessRatesAttachmentService
                  .persistSessionData(
                    request.ses.copy(hasRatesBill = Some(hasRatesBill), uploadEvidenceData = UploadEvidenceData.empty)
                  )
                  .map(_ => Redirect(routes.UploadController.show(evidence)))
            }
          }
        )
    }

  private def getEvidenceChoice(evidenceType: Option[EvidenceType] = None): EvidenceChoices =
    evidenceType match {
      case Some(RatesBillType)        => EvidenceChoices.RATES_BILL
      case Some(Lease)                => EvidenceChoices.LEASE
      case Some(License)              => EvidenceChoices.LICENSE
      case Some(ServiceCharge)        => EvidenceChoices.SERVICE_CHARGE
      case Some(StampDutyLandTaxForm) => EvidenceChoices.STAMP_DUTY
      case Some(LandRegistryTitle)    => EvidenceChoices.LAND_REGISTRY
      case Some(WaterRateDemand)      => EvidenceChoices.WATER_RATE
      case Some(OtherUtilityBill)     => EvidenceChoices.UTILITY_RATE

    }

  def submitOccupierForm: Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      def updateSession(newAnswer: EvidenceType): Future[Unit] =
        // if the same answer was already in the linking session, then do nothing
        if (request.ses.occupierEvidenceType.contains(newAnswer)) Future.successful((): Unit)
        else
          businessRatesAttachmentService.persistSessionData(
            request.ses.copy(occupierEvidenceType = Some(newAnswer), uploadEvidenceData = UploadEvidenceData.empty)
          )

      OccupierEvidenceType.form
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(chooseOccupierEvidenceView(errors, Some(backlink(request.ses))))),
          formData =>
            updateSession(formData).map { _ =>
              val choice =
                if (formData == NoLeaseOrLicense) EvidenceChoices.NO_LEASE_OR_LICENSE
                else if (formData == Lease) EvidenceChoices.LEASE
                else EvidenceChoices.LICENSE
              if (request.ses.evidenceType.contains(formData))
                request.ses.uploadEvidenceData.fileInfo match {
                  case Some(CompleteFileInfo(_, _)) =>
                    Redirect(routes.UploadResultController.show(choice))
                  case _ =>
                    Redirect(routes.UploadController.show(choice))
                }
              else
                Redirect(routes.UploadController.show(choice))
            }
        )
    }
}

object ChooseEvidence {
  lazy val form = Form(single("hasRatesBill" -> mandatoryBoolean))
}
