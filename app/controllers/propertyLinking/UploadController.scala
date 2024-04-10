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
import actions.propertylinking.requests.LinkingSessionRequest
import binders.propertylinks.EvidenceChoices
import binders.propertylinks.EvidenceChoices.{EvidenceChoices, RATES_BILL}
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models.EvidenceType.form
import models._
import models.attachment.request.{InitiateAttachmentRequest, UpscanInitiateRequest}
import models.attachment.{Destinations, InitiateAttachmentPayload}
import models.upscan.PreparedUpload
import play.api.Logging
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UploadController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      @Named("propertyLinkingSession") sessionRepository: SessionRepo,
      businessRatesAttachmentsService: BusinessRatesAttachmentsService,
      uploadView: views.html.propertyLinking.upload,
      uploadOtherEvidenceView: views.html.propertyLinking.uploadEvidence,
      uploadResultView: views.html.propertyLinking.upload_result,
      cannotProvideEvidenceView: views.html.propertyLinking.cannotProvideEvidence
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      applicationConfig: ApplicationConfig
) extends PropertyLinkingController with Logging {

  def show(
        evidence: EvidenceChoices,
        errorCode: Option[String],
        fileRemoved: Boolean,
        removedFileName: Option[String]): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      val session = request.ses
      evidence match {
        case EvidenceChoices.RATES_BILL | EvidenceChoices.LEASE | EvidenceChoices.LICENSE |
            EvidenceChoices.SERVICE_CHARGE | EvidenceChoices.STAMP_DUTY | EvidenceChoices.LAND_REGISTRY |
            EvidenceChoices.WATER_RATE | EvidenceChoices.UTILITY_RATE =>
          val linkBasis = if (evidence == RATES_BILL) RatesBillFlag else OtherEvidenceFlag
          for {
            preparedUpload <- requestNewUpscanForm(evidence)
            _ <- businessRatesAttachmentsService.persistSessionData(
                  request.ses
                    .copy(fileReference = Some(preparedUpload.reference.value))
                    .copy(evidenceType = Some(getEvidenceType(evidence))),
                  request.ses.uploadEvidenceData.copy(linkBasis = linkBasis)
                )
          } yield {
            Ok(
              uploadView(
                getEvidenceType(evidence),
                evidence,
                session.submissionId,
                errorCode,
                preparedUpload,
                fileRemoved,
                removedFileName
              ))
          }
        case EvidenceChoices.OTHER | EvidenceChoices.NO_LEASE_OR_LICENSE =>
          Future.successful(
            Ok(uploadOtherEvidenceView(
              session.submissionId,
              session.uploadEvidenceData.attachments.getOrElse(Map.empty),
              session.uploadEvidenceData.fileInfo
                .map(x => form.fill(x.evidenceType))
                .getOrElse(form),
              session
            )))
        case _ =>
          Future.successful(BadRequest(errorHandler.badRequestTemplate))
      }
    }

  def initiate(evidence: EvidenceChoices): Action[JsValue] =
    authenticatedAction.andThen(withLinkingSession).async(parse.json) { implicit request =>
      withJsonBody[InitiateAttachmentRequest] { attachmentRequest =>
        businessRatesAttachmentsService
          .initiateAttachmentUpload(
            InitiateAttachmentPayload(
              request = attachmentRequest,
              successRedirectUrl = applicationConfig.serviceUrl + routes.UploadController.show(evidence).url,
              errorRedirectUrl = applicationConfig.serviceUrl + routes.UploadController.upscanFailure(evidence, None)
            ),
            attachmentRequest.evidenceType
          )
          .map(response => Ok(Json.toJson(response)))
          .recover {
            case ex @ UpstreamErrorResponse.WithStatusCode(BAD_REQUEST) =>
              logger.warn(s"Initiate Upload was Bad Request: ${ex.message}")
              BadRequest(Messages("error.businessRatesAttachment.does.not.support.file.types"))
            case ex: Exception =>
              logger.warn("FileAttachmentFailed Exception:", ex)
              InternalServerError("500 INTERNAL_SERVER_ERROR")
          }
      }
    }

  def updateEvidenceType: Action[JsValue] =
    authenticatedAction.andThen(withLinkingSession).async(parse.json) { implicit request =>
      form
        .bindFromRequest()
        .fold(
          hasErrors = _ =>
            Future.successful(BadRequest(uploadOtherEvidenceView(
              request.ses.submissionId,
              request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
              form.withError(FormError("evidenceType", "error.businessRatesAttachment.evidence.not.selected")),
              request.ses
            ))),
          evidenceType => {
            val updatedSession = request.ses.copy(evidenceType = Some(evidenceType))
            val fileInfo = request.ses.uploadEvidenceData.fileInfo match {
              case Some(CompleteFileInfo(name, _)) => CompleteFileInfo(name, evidenceType)
              case _                               => PartialFileInfo(evidenceType)
            }

            val updatedUploadData: UploadEvidenceData = UploadEvidenceData(
              linkBasis = if (evidenceType == RatesBillType) RatesBillFlag else OtherEvidenceFlag,
              fileInfo = Some(fileInfo),
              attachments = request.ses.uploadEvidenceData.attachments
            )

            businessRatesAttachmentsService
              .persistSessionData(updatedSession, updatedUploadData)
              .map(
                _ =>
                  Ok(
                    uploadOtherEvidenceView(
                      request.ses.submissionId,
                      request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
                      form.fill(evidenceType),
                      request.ses
                    )))
          }
        )
    }

  def continue: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        _ =>
          Future.successful(BadRequest(uploadOtherEvidenceView(
            submissionId = request.ses.submissionId,
            uploadedFiles = request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
            formEvidence =
              form.withError(FormError("evidenceType", "error.businessRatesAttachment.evidence.not.selected")),
            linkingSession = request.ses
          ))), {
          case UnableToProvide =>
            businessRatesAttachmentsService
              .persistSessionData(
                request.ses.copy(evidenceType = Some(UnableToProvide)),
                request.ses.uploadEvidenceData
                  .copy(fileInfo = Some(PartialFileInfo(UnableToProvide))))
              .map(_ => Redirect(routes.UploadController.cannotProvideEvidence))
          case formData if request.ses.evidenceType.contains(formData) =>
            request.ses.uploadEvidenceData.fileInfo match {
              case Some(CompleteFileInfo(_, _)) =>
                Future.successful(Redirect(routes.UploadResultController.show(getEvidenceChoice(Some(formData)))))
              case _ =>
                Future.successful(Redirect(routes.UploadController.show(getEvidenceChoice(Some(formData)))))

            }
          case formData =>
            businessRatesAttachmentsService
              .persistSessionData(
                request.ses.copy(evidenceType = Some(formData)),
                request.ses.uploadEvidenceData
                  .copy(fileInfo = Some(PartialFileInfo(formData)), linkBasis = OtherEvidenceFlag)
              )
              .map(_ => Redirect(routes.UploadController.show(getEvidenceChoice(Some(formData)))))
        }
      )
  }

  private def requestNewUpscanForm(evidence: EvidenceChoices)(
        implicit request: LinkingSessionRequest[_],
        hc: HeaderCarrier): Future[PreparedUpload] = {
    val initiateUploadRequest =
      UpscanInitiateRequest(
        successRedirect = applicationConfig.serviceUrl + routes.UploadResultController.show(evidence),
        errorRedirect = applicationConfig.serviceUrl + routes.UploadController.upscanFailure(evidence, None),
        Destinations.PROPERTY_LINK_EVIDENCE_DFE
      )

    businessRatesAttachmentsService.initiateUpload(initiateUploadRequest)
  }

  private def getEvidenceType(evidence: EvidenceChoices, evidenceType: Option[EvidenceType] = None): EvidenceType =
    evidence match {
      case EvidenceChoices.RATES_BILL     => RatesBillType
      case EvidenceChoices.LEASE          => Lease
      case EvidenceChoices.LICENSE        => License
      case EvidenceChoices.RATES_BILL     => RatesBillType
      case EvidenceChoices.SERVICE_CHARGE => ServiceCharge
      case EvidenceChoices.STAMP_DUTY     => StampDutyLandTaxForm
      case EvidenceChoices.LAND_REGISTRY  => LandRegistryTitle
      case EvidenceChoices.WATER_RATE     => WaterRateDemand
      case EvidenceChoices.UTILITY_RATE   => OtherUtilityBill
      case _                              => evidenceType.get
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

  /*
    When this method is hit it will clear down ALL files in the session, this is only safe in property linking as there is only allowed one file.

    Upscan should return the fileReference with the error ???
   */
  def upscanFailure(
        evidence: EvidenceChoices,
        errorCode: Option[String],
        errorMessage: Option[String]): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      val session = request.ses
      logger.info(s"Upscan failed with ErrorCode: $errorCode, ErrorMessage: $errorMessage ")
      businessRatesAttachmentsService
        .persistSessionData(
          session.copy(evidenceType = None),
          session.uploadEvidenceData.copy(attachments = Some(Map.empty)))
        .map(_ => Redirect(routes.UploadController.show(evidence, errorCode)))
    }

  def remove(evidence: EvidenceChoices): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      val session = request.ses
      val optFileName = session.uploadEvidenceData.fileInfo match {
        case Some(completeFileInfo: CompleteFileInfo) => Some(completeFileInfo.name)
        case _                                        => None
      }
      businessRatesAttachmentsService
        .persistSessionData(
          request.ses,
          session.uploadEvidenceData.copy(
            fileInfo = Option(PartialFileInfo(getEvidenceType(evidence, request.ses.evidenceType))),
            attachments = Some(Map.empty))
        )
        .map(_ => Redirect(routes.UploadController.show(evidence, fileRemoved = true, removedFileName = optFileName)))
    }

  def cannotProvideEvidence(): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      Future.successful(Ok(cannotProvideEvidenceView()))
    }
}
