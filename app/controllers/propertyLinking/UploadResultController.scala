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
import actions.propertylinking.WithLinkingSession
import binders.propertylinks.EvidenceChoices
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import cats.data.OptionT
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models._
import models.attachment.Attachment
import models.upscan.FileStatus.FileStatus
import models.upscan.{FileMetadata, FileStatus, UploadedFileDetails}
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepo
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UploadResultController @Inject()(
                                  val errorHandler: CustomErrorHandler,
                                  authenticatedAction: AuthenticatedAction,
                                  withLinkingSession: WithLinkingSession,
                                  @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                                  businessRatesAttachmentsService: BusinessRatesAttachmentsService,
                                  uploadResultView: views.html.propertyLinking.upload_result,
                                )(
                                  implicit executionContext: ExecutionContext,
                                  override val messagesApi: MessagesApi,
                                  override val controllerComponents: MessagesControllerComponents,
                                  applicationConfig: ApplicationConfig
                                ) extends PropertyLinkingController with Logging {

  def show(evidence: EvidenceChoices): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>

      def resultPage(fileStatus: FileStatus, attachment: Option[Attachment] = None): Result = {
        Ok(
          uploadResultView(
            getEvidenceType(evidence),
            evidence,
            request.ses.submissionId,
            request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
            fileStatus,
            request.ses.fileReference,
            attachment
          )
        )
      }

      def updatedUploadData(attachment: Attachment): UploadEvidenceData = {

        val evidenceChoice = getEvidenceType(evidence)
        val fileInfo = request.ses.uploadEvidenceData.fileInfo match {
          case Some(CompleteFileInfo(name, _)) => CompleteFileInfo(name, evidenceChoice)
          case _ => CompleteFileInfo(attachment.fileName, evidenceChoice)
        }

        UploadEvidenceData(
          linkBasis = if (evidenceChoice == RatesBillType) RatesBillFlag else OtherEvidenceFlag,
          fileInfo = Some(fileInfo),
          attachments = request.ses.uploadEvidenceData.attachments
        )
      }

      val result: OptionT[Future, Attachment] = for {
        reference <- OptionT(Future.successful(request.ses.fileReference))
        attachment <- OptionT.liftF(businessRatesAttachmentsService.getAttachment(reference))
        _ <- OptionT.liftF(sessionRepository.saveOrUpdate(request.ses.copy(uploadEvidenceData = updatedUploadData(attachment))))
        fullAttachment = attachment
      } yield {
        fullAttachment
      }

      result.value.map {
        case Some(attachment) =>
          val fileStatus = attachment.scanResult.map(_.fileStatus).getOrElse(FileStatus.UPLOADING)
          resultPage(fileStatus, Some(attachment))
        case None =>
          //todo I think this needs to be something else
          BadRequest(errorHandler.badRequestTemplate)
      }.recover {
        case _ @ UpstreamErrorResponse.WithStatusCode(EXPECTATION_FAILED) =>
          resultPage(FileStatus.UPLOADING)
      }
    }


  def submit(evidenceChoices: EvidenceChoices, fileStatus: String): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    val status = FileStatus.withName(fileStatus)
    status match {
            case FileStatus.READY =>

              val result: Future[Result] = (
                for {
                  reference <- OptionT(Future.successful(request.ses.fileReference))
                  attachment <- OptionT.liftF(businessRatesAttachmentsService.getAttachment(reference))
                  preparedUpload <- OptionT(Future.successful(attachment.initiateResult))
                  map = Map(
                    reference -> UploadedFileDetails(
                      FileMetadata(attachment.fileName, attachment.mimeType),
                      preparedUpload))
                  _ <- OptionT.liftF(
                    businessRatesAttachmentsService.persistSessionData(
                      request.ses.copy(evidenceType = Some(getEvidenceType(evidenceChoices))),
                      request.ses.uploadEvidenceData.copy(attachments = Some(map)))
                  )
                  result <- OptionT.pure[Future](Redirect(routes.DeclarationController.show))
                } yield result
                ).getOrElse(Redirect(routes.UploadController.show(evidenceChoices)))

              result

            case FileStatus.UPLOADING =>
              Future.successful(BadRequest(uploadResultView(
              getEvidenceType(evidenceChoices),
              evidenceChoices,
              request.ses.submissionId,
              request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
              status,
              request.ses.fileReference,
              request.ses.attachment,
              notReadyError = true)))
            case FileStatus.FAILED =>
              Future.successful(Redirect(routes.UploadController.show(evidenceChoices)))
          }
    }

  private def getEvidenceType(evidence: EvidenceChoices, evidenceType: Option[EvidenceType] = None): EvidenceType =
    evidence match {
      case EvidenceChoices.RATES_BILL => RatesBillType
      case EvidenceChoices.LEASE => Lease
      case EvidenceChoices.LICENSE => License
      case EvidenceChoices.RATES_BILL => RatesBillType
      case EvidenceChoices.SERVICE_CHARGE => ServiceCharge
      case EvidenceChoices.STAMP_DUTY => StampDutyLandTaxForm
      case EvidenceChoices.LAND_REGISTRY => LandRegistryTitle
      case EvidenceChoices.WATER_RATE => WaterRateDemand
      case EvidenceChoices.UTILITY_RATE => OtherUtilityBill
      case EvidenceChoices.OTHER => evidenceType.get
    }

  private def getEvidenceChoice(evidenceType: Option[EvidenceType] = None): EvidenceChoices =
    evidenceType match {
      case Some(RatesBillType) => EvidenceChoices.RATES_BILL
      case Some(Lease) => EvidenceChoices.LEASE
      case Some(License) => EvidenceChoices.LICENSE
      case Some(ServiceCharge) => EvidenceChoices.SERVICE_CHARGE
      case Some(StampDutyLandTaxForm) => EvidenceChoices.STAMP_DUTY
      case Some(LandRegistryTitle) => EvidenceChoices.LAND_REGISTRY
      case Some(WaterRateDemand) => EvidenceChoices.WATER_RATE
      case Some(OtherUtilityBill) => EvidenceChoices.UTILITY_RATE

    }

  def getUploadStatus: Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>

      val result: OptionT[Future, Attachment] = for {
        reference <- OptionT(Future.successful(request.ses.fileReference))
        attachment <- OptionT.liftF(businessRatesAttachmentsService.getAttachment(reference))
        fullAttachment = attachment
      } yield {
        fullAttachment
      }
      result.value.map {
        case Some(attachment) =>
          val fileStatus = attachment.scanResult.map(_.fileStatus).getOrElse(FileStatus.UPLOADING)
          println(Console.GREEN + fileStatus + Console.RESET)
          Ok(Json.toJson(fileStatus))
        case None =>
          //todo I think this needs to be something else
          BadRequest(errorHandler.badRequestTemplate)
      }.recover {
        case _@UpstreamErrorResponse.WithStatusCode(EXPECTATION_FAILED) =>
          println(Console.YELLOW + "Uploading" + Console.RESET)
          Ok(Json.toJson(FileStatus.UPLOADING))
      }
    }


}
