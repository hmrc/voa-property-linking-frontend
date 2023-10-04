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
import binders.propertylinks.EvidenceChoices.{EvidenceChoices, Value}
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models.EvidenceType.form
import models._
import models.attachment.InitiateAttachmentPayload
import models.attachment.request.InitiateAttachmentRequest
import play.api.Logging
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      businessRatesAttachmentsService: BusinessRatesAttachmentsService,
      uploadRatesBillLeaseOrLicenseView: views.html.propertyLinking.uploadRatesBillLeaseOrLicense,
      uploadEvidenceView: views.html.propertyLinking.uploadEvidence,
      cannotProvideEvidenceView: views.html.propertyLinking.cannotProvideEvidence
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      applicationConfig: ApplicationConfig
) extends PropertyLinkingController with Logging {

  def show(evidence: EvidenceChoices, errorMessage: Option[String]): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession) { implicit request =>
      val session = request.ses
      evidence match {
        case EvidenceChoices.RATES_BILL | EvidenceChoices.LEASE | EvidenceChoices.LICENSE |
            EvidenceChoices.SERVICE_CHARGE | EvidenceChoices.STAMP_DUTY | EvidenceChoices.LAND_REGISTRY |
            EvidenceChoices.WATER_RATE | EvidenceChoices.UTILITY_RATE => {
          Ok(
            uploadRatesBillLeaseOrLicenseView(
              getEvidenceType(evidence),
              evidence,
              session.submissionId,
              upscanErrors(errorMessage).toList,
              session.uploadEvidenceData.attachments.getOrElse(Map.empty)
            ))
        }
        case EvidenceChoices.OTHER | EvidenceChoices.NO_LEASE_OR_LICENSE =>
          Ok(
            uploadEvidenceView(
              session.submissionId,
              upscanErrors(errorMessage).toList,
              session.uploadEvidenceData.attachments.getOrElse(Map.empty),
              session.uploadEvidenceData.fileInfo
                .map(x => form.fill(x.evidenceType))
                .getOrElse(form),
              session
            ))
        case _ =>
          BadRequest(errorHandler.badRequestTemplate)
      }
    }

  def initiate(evidence: EvidenceChoices): Action[JsValue] =
    authenticatedAction.andThen(withLinkingSession).async(parse.json) { implicit request =>
      withJsonBody[InitiateAttachmentRequest] { attachmentRequest =>
        businessRatesAttachmentsService
          .initiateAttachmentUpload(
            InitiateAttachmentPayload(
              attachmentRequest,
              applicationConfig.serviceUrl + routes.UploadController.show(evidence).url,
              applicationConfig.serviceUrl + routes.UploadController.upscanFailure(evidence, None)
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
            Future.successful(BadRequest(uploadEvidenceView(
              request.ses.submissionId,
              List.empty,
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
                    uploadEvidenceView(
                      request.ses.submissionId,
                      List.empty,
                      request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
                      form.fill(evidenceType),
                      request.ses
                    )))
          }
        )
    }

  def continue(evidence: EvidenceChoices): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async {
    implicit request =>
      def upload(uploadedData: UploadEvidenceData, linkingSession: LinkingSession = request.ses)(
            implicit request: LinkingSessionRequest[_]): Option[Future[Result]] =
        PartialFunction.condOpt(request.ses.uploadEvidenceData.attachments) {
          case Some(fileData) if fileData.nonEmpty =>
            val fileInfo: FileInfo = uploadedData.fileInfo match {
              case Some(CompleteFileInfo(name, _)) => CompleteFileInfo(name, getEvidenceType(evidence))
              case _                               => PartialFileInfo(getEvidenceType(evidence))
            }

            businessRatesAttachmentsService
              .persistSessionData(linkingSession, uploadedData.copy(fileInfo = Some(fileInfo)))
              .map(_ => Redirect(routes.DeclarationController.show.url))
        }
      val session: LinkingSession = request.ses
      evidence match {
        case EvidenceChoices.RATES_BILL | EvidenceChoices.LEASE | EvidenceChoices.LICENSE =>
          upload(session.uploadEvidenceData.copy(linkBasis = RatesBillFlag))
            .getOrElse(
              Future.successful(
                BadRequest(
                  uploadRatesBillLeaseOrLicenseView(
                    getEvidenceType(evidence),
                    evidence,
                    request.ses.submissionId,
                    List("error.businessRatesAttachment.ratesBill.not.selected"),
                    Map())))
            )
        case EvidenceChoices.SERVICE_CHARGE | EvidenceChoices.STAMP_DUTY | EvidenceChoices.LAND_REGISTRY |
            EvidenceChoices.WATER_RATE | EvidenceChoices.UTILITY_RATE =>
          val updatedSession = session.copy(evidenceType = Some(getEvidenceType(evidence)))
          val sessionUploadData: UploadEvidenceData = updatedSession.uploadEvidenceData
            .copy(linkBasis = if (getEvidenceType(evidence) == RatesBillType) RatesBillFlag else OtherEvidenceFlag)
          upload(sessionUploadData)
            .getOrElse(
              Future.successful(
                BadRequest(
                  uploadRatesBillLeaseOrLicenseView(
                    getEvidenceType(evidence),
                    evidence,
                    request.ses.submissionId,
                    List("error.businessRatesAttachment.ratesBill.not.selected"),
                    Map())
                )
              )
            )
        case EvidenceChoices.OTHER =>
          form
            .bindFromRequest()
            .fold(
              _ =>
                Future.successful(BadRequest(uploadEvidenceView(
                  submissionId = request.ses.submissionId,
                  errors = Nil,
                  uploadedFiles = request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
                  formEvidence =
                    form.withError(FormError("evidenceType", "error.businessRatesAttachment.evidence.not.selected")),
                  linkingSession = session
                ))), {
                case UnableToProvide =>
                  Future.successful(Ok(cannotProvideEvidenceView()))
                case formData =>
                  Future.successful(Redirect(routes.UploadController.show(getEvidenceChoice(Some(formData)))))
              }
            )
        case _ =>
          Future.successful(BadRequest(errorHandler.badRequestTemplate))
      }
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
      case EvidenceChoices.OTHER          => evidenceType.get
    }

  //Catching error message received from Upscan & replacing
  private def upscanErrors(errorMessage: Option[String])(implicit messages: Messages): Option[String] =
    if (errorMessage.contains("Your proposed upload exceeds the maximum allowed size"))
      Some(messages("error.businessRatesAttachment.file.size.exceed.max.limit"))
    else errorMessage

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
  def upscanFailure(evidence: EvidenceChoices, errorMessage: Option[String]): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      val session = request.ses

      businessRatesAttachmentsService
        .persistSessionData(
          session.copy(evidenceType = None),
          session.uploadEvidenceData.copy(attachments = Some(Map.empty)))
        .map(_ => Redirect(routes.UploadController.show(evidence, errorMessage)))
    }

  def remove(
        fileReference: String,
        evidence: EvidenceChoices
  ): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    val session = request.ses
    val updatedSessionData =
      session.uploadEvidenceData.attachments.map(map => map - fileReference).getOrElse(Map.empty)

    businessRatesAttachmentsService
      .persistSessionData(
        session.copy(evidenceType = None),
        session.uploadEvidenceData.copy(attachments = Some(updatedSessionData)))
      .map(_ => Redirect(routes.UploadController.show(evidence)))
  }
}
