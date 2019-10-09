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
import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import binders.propertylinks.EvidenceChoices
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import config.ApplicationConfig
import connectors.attachments.errorhandler.exceptions.FileAttachmentFailed
import controllers.PropertyLinkingController
import javax.inject.Inject
import models.EvidenceType.form
import models._
import models.attachment.InitiateAttachmentPayload
import models.attachment.request.InitiateAttachmentRequest
import play.api.Logger
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import services.BusinessRatesAttachmentService
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler
import views.html.propertyLinking.{uploadEvidence, uploadRatesBill}

import scala.concurrent.{ExecutionContext, Future}

class UploadController @Inject()(
                                val errorHandler: CustomErrorHandler,
                                authenticatedAction: AuthenticatedAction,
                                withLinkingSession: WithLinkingSession,
                                businessRatesAttachmentsServices: BusinessRatesAttachmentService
                                )(implicit executionContext: ExecutionContext, val messagesApi: MessagesApi, applicationConfig: ApplicationConfig) extends PropertyLinkingController {

  def show(evidence: EvidenceChoices, errorMessage: Option[String]): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession) { implicit request =>

    val session = request.ses
    evidence match {
      case EvidenceChoices.RATES_BILL =>
        Ok(uploadRatesBill(session.submissionId, errorMessage.toList, session.uploadEvidenceData.attachments.getOrElse(Map.empty))).withHeaders("Access-Control-Allow-Origin" -> "*")
      case EvidenceChoices.OTHER      =>
        Ok(uploadEvidence(session.submissionId, errorMessage.toList, session.uploadEvidenceData.attachments.getOrElse(Map.empty), session.evidenceType.map(x => form.fill(x)).getOrElse(form)))
      case _                          =>
        BadRequest(errorHandler.badRequestTemplate)
    }
  }

  def initiate(evidence: EvidenceChoices): Action[JsValue] = authenticatedAction.andThen(withLinkingSession).async(parse.json) { implicit request =>
    withJsonBody[InitiateAttachmentRequest] { attachmentRequest =>
      businessRatesAttachmentsServices
        .initiateAttachmentUpload(InitiateAttachmentPayload(attachmentRequest, applicationConfig.serviceUrl + routes.UploadController.show(evidence).url, applicationConfig.serviceUrl + routes.UploadController.upscanFailure(evidence, None)))
        .map(response => Ok(Json.toJson(response)))
        .recover {
          case _: FileAttachmentFailed  =>
            BadRequest(Json.toJson(Messages("error.businessRatesAttachment.does.not.support.file.types")))
          case ex: Exception            =>
            Logger.warn("FileAttachmentFailed Exception:", ex)
            InternalServerError("500 INTERNAL_SERVER_ERROR")
        }
    }
  }

  def continue(evidence: EvidenceChoices): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    def upload(uploadedData: UploadEvidenceData)(implicit request: LinkingSessionRequest[_]): Option[Future[Result]] = {
      PartialFunction.condOpt(request.ses.uploadEvidenceData.attachments) {
        case Some(fileData) if fileData.nonEmpty =>
          businessRatesAttachmentsServices.persistSessionData(request.ses, uploadedData).map(x => Redirect(routes.Declaration.show().url))
      }
    }

    val session = request.ses
    evidence match {
      case EvidenceChoices.RATES_BILL =>
        upload(session.uploadEvidenceData.copy(linkBasis = RatesBillFlag, fileInfo = session.uploadEvidenceData.fileInfo.map(_.copy(evidenceType = RatesBillType))))
          .getOrElse(Future.successful(BadRequest(uploadRatesBill(request.ses.submissionId, List("error.businessRatesAttachment.file.not.selected"), Map()))))
      case EvidenceChoices.OTHER =>
        form.bindFromRequest().fold(
          _ =>
            Future.successful(BadRequest(uploadEvidence(request.ses.submissionId, List("error.businessRatesAttachment.evidence.not.selected"), request.ses.uploadEvidenceData.attachments.getOrElse(Map()), form))),
          formData => {
            val updatedSession = session.copy(evidenceType = EvidenceType.fromName(formData.name))
            val sessionUploadData: UploadEvidenceData = updatedSession.uploadEvidenceData
              .copy(
                linkBasis = OtherEvidenceFlag,
                fileInfo = updatedSession.uploadEvidenceData.fileInfo.map(_.copy(evidenceType = formData)))
            upload(sessionUploadData)
              .getOrElse(Future.successful(BadRequest(uploadEvidence(request.ses.submissionId, List("error.businessRatesAttachment.file.not.selected"), Map(), form))))
          })
      case _ =>
        Future.successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  /*
    When this method is hit it will clear down ALL files in the session, this is only safe in property linking as there is only allowed one file.

    Upscan should return the fileReference with the error ???
   */
  def upscanFailure(evidence: EvidenceChoices, errorMessage: Option[String]): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    val session = request.ses

    businessRatesAttachmentsServices
      .persistSessionData(session.copy(evidenceType = None), session.uploadEvidenceData.copy(attachments = Some(Map.empty)))
      .map( _ => Redirect(routes.UploadController.show(evidence, errorMessage)))
  }

  def remove(
              fileReference: String,
              evidence: EvidenceChoices
            ): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    val session = request.ses
    val updatedSessionData = session.uploadEvidenceData.attachments.map(map => map - fileReference).getOrElse(Map.empty)

    businessRatesAttachmentsServices
      .persistSessionData(session.copy(evidenceType = None), session.uploadEvidenceData.copy(attachments = Some(updatedSessionData)))
      .map( _ => Redirect(routes.UploadController.show(evidence)))
  }
}