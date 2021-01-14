/*
 * Copyright 2021 HM Revenue & Customs
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
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import config.ApplicationConfig
import controllers.PropertyLinkingController
import javax.inject.Inject

import models.EvidenceType.form
import models._
import models.attachment.InitiateAttachmentPayload
import models.attachment.request.InitiateAttachmentRequest
import play.api.Logger
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class UploadController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      businessRatesAttachmentsService: BusinessRatesAttachmentsService,
      uploadRatesBillView: views.html.propertyLinking.uploadRatesBill,
      uploadEvidenceView: views.html.propertyLinking.uploadEvidence
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      applicationConfig: ApplicationConfig
) extends PropertyLinkingController {

  def show(evidence: EvidenceChoices, errorMessage: Option[String]): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession) { implicit request =>
      val session = request.ses
      evidence match {
        case EvidenceChoices.RATES_BILL =>
          Ok(
            uploadRatesBillView(
              session.submissionId,
              errorMessage.toList,
              session.uploadEvidenceData.attachments.getOrElse(Map.empty),
              session
            ))
            .withHeaders("Access-Control-Allow-Origin" -> "*")
        case EvidenceChoices.OTHER =>
          Ok(
            uploadEvidenceView(
              session.submissionId,
              errorMessage.toList,
              session.uploadEvidenceData.attachments.getOrElse(Map.empty),
              session.uploadEvidenceData.fileInfo
                .map(x => x.evidenceType.fold(form)(e => form.fill(e)))
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
          .initiateAttachmentUpload(InitiateAttachmentPayload(
            attachmentRequest,
            applicationConfig.serviceUrl + routes.UploadController.show(evidence).url,
            applicationConfig.serviceUrl + routes.UploadController.upscanFailure(evidence, None)
          ))
          .map(response => Ok(Json.toJson(response)))
          .recover {
            case UpstreamErrorResponse.WithStatusCode(BAD_REQUEST, ex) =>
              Logger.warn(s"Initiate Upload was Bad Request: ${ex.message}")
              BadRequest(Json.toJson(Messages("error.businessRatesAttachment.does.not.support.file.types")))
            case ex: Exception =>
              Logger.warn("FileAttachmentFailed Exception:", ex)
              InternalServerError("500 INTERNAL_SERVER_ERROR")
          }
      }
    }

  def continue(evidence: EvidenceChoices): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async {
    implicit request =>
      def upload(uploadedData: UploadEvidenceData)(implicit request: LinkingSessionRequest[_]): Option[Future[Result]] =
        PartialFunction.condOpt(request.ses.uploadEvidenceData.attachments) {
          case Some(fileData) if fileData.nonEmpty =>
            businessRatesAttachmentsService
              .persistSessionData(request.ses, uploadedData)
              .map(x => Redirect(routes.DeclarationController.show().url))
        }

      val session = request.ses
      evidence match {
        case EvidenceChoices.RATES_BILL =>
          upload(
            session.uploadEvidenceData.copy(
              linkBasis = RatesBillFlag,
              fileInfo = session.uploadEvidenceData.fileInfo.map(_.copy(evidenceType = Some(RatesBillType)))))
            .getOrElse(
              Future.successful(
                BadRequest(
                  uploadRatesBillView(
                    request.ses.submissionId,
                    List("error.businessRatesAttachment.file.not.selected"),
                    Map(),
                    session))))
        case EvidenceChoices.OTHER =>
          form
            .bindFromRequest()
            .fold(
              _ =>
                Future.successful(BadRequest(uploadEvidenceView(
                  request.ses.submissionId,
                  PartialFunction
                    .condOpt(session.uploadEvidenceData.attachments) {
                      case Some(fileData) if fileData.isEmpty =>
                        List("error.businessRatesAttachment.file.not.selected")
                    }
                    .getOrElse(Nil),
                  request.ses.uploadEvidenceData.attachments.getOrElse(Map()),
                  form.withError(FormError("evidenceType", "error.businessRatesAttachment.evidence.not.selected")),
                  session
                ))),
              formData => {
                val updatedSession = session.copy(evidenceType = EvidenceType.fromName(formData.name))
                val sessionUploadData: UploadEvidenceData = updatedSession.uploadEvidenceData
                  .copy(
                    linkBasis = OtherEvidenceFlag,
                    fileInfo = updatedSession.uploadEvidenceData.fileInfo.map(_.copy(evidenceType = Some(formData))))
                upload(sessionUploadData)
                  .getOrElse(
                    Future.successful(
                      BadRequest(
                        uploadEvidenceView(
                          request.ses.submissionId,
                          List("error.businessRatesAttachment.file.not.selected"),
                          Map(),
                          form.fill(formData),
                          session))))
              }
            )
        case _ =>
          Future.successful(BadRequest(errorHandler.badRequestTemplate))
      }
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
