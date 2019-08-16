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

import javax.inject.Inject
import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors.FileAttachmentFailed
import controllers._
import form.{EnumMapping}
import form.Mappings._
import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.PropertyLinkEvidence
import models._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, Request}
import services.BusinessRatesAttachmentService
import session.WithLinkingSession
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.frontend.controller.Utf8MimeTypes
import views.helpers.Errors
import views.html.propertyLinking.uploadEvidence

import scala.concurrent.Future

class UploadPropertyEvidence @Inject()(authenticated: AuthenticatedAction, val withLinkingSession: WithLinkingSession, businessRatesAttachmentService: BusinessRatesAttachmentService)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with BaseController with Utf8MimeTypes {

  def show() = withLinkingSession { implicit request =>
    Ok(uploadEvidence(request.ses.submissionId, List.empty, request.ses.uploadEvidenceData.attachments.getOrElse(Map.empty), form))}


  def initiate(): Action[JsValue] = authenticated.async(parse.json) { implicit request  =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

   val parseResult:JsResult[InitiateAttachmentRequest] = request.body.validate[InitiateAttachmentRequest]

    parseResult match {
      case JsSuccess(initiateRequest: InitiateAttachmentRequest, _: JsPath) =>
         (
          for {
            initiateAttachmentResult <- businessRatesAttachmentService.initiateAttachmentUpload(initiateRequest.copy(destination = Some(PropertyLinkEvidence.destination)))(request, hc)
          } yield
            Ok(Json.toJson(initiateAttachmentResult))
          ).recover {
          case fileAttachmentFailed: FileAttachmentFailed =>
            Logger.warn("FileAttachmentFailed Bad Request Exception:" + fileAttachmentFailed.errorMessage)
            BadRequest(Json.toJson(Messages("error.businessRatesAttachment.does.not.support.file.types")))
          case ex: Exception =>
            Logger.warn("FileAttachmentFailed Exception:" + ex.getMessage)
            InternalServerError("500 INTERNAL_SERVER_ERROR")
        }
      case error: JsError =>
        Logger.info(s"---> BadRequest: ${JsError.toJson(error).toString}")
        Future.successful(BadRequest(Json.obj(
          "error" -> s"JSON initiate Failed",
          "messages" -> Json.arr(Json.obj("error" -> JsError.toJson(error).toString())))))
    }

  }

    def removeFile(fileReference: String) = withLinkingSession { implicit request =>
      implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      val updatedSessionData = request.ses.uploadEvidenceData.attachments.map(map => map - fileReference)

     for{
        - <- businessRatesAttachmentService.persistSessionData(request.ses, request.ses.uploadEvidenceData.copy( attachments = updatedSessionData))
      }yield (Ok(uploadEvidence(request.ses.submissionId, List.empty, updatedSessionData.getOrElse(Map()), form)))
  }


  def continue() = withLinkingSession { implicit request =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    request.ses.uploadEvidenceData.attachments match {
      case Some(fileData) if fileData.size > 0 => {
        form.bindFromRequest().fold(
          errors => BadRequest(uploadEvidence(request.ses.submissionId, List("error.businessRatesAttachment.evidence.not.selected"), request.ses.uploadEvidenceData.attachments.getOrElse(Map.empty), form)),
          formData => {
            val sessionUploadData: UploadEvidenceData =
              request.ses.uploadEvidenceData.copy(linkBasis = OtherEvidenceFlag, fileInfo = request.ses.uploadEvidenceData.fileInfo.map(x => x.copy(evidenceType = EvidenceType.fromName(formData.name).get)))
            businessRatesAttachmentService.persistSessionData(request.ses, sessionUploadData).map(x => Redirect(routes.Declaration.show().url))
          })
      }
      case _ =>
        BadRequest(uploadEvidence(request.ses.submissionId, List("error.businessRatesAttachment.file.not.selected"), Map(), form))
    }
  }
  lazy val form = Form(single("evidenceType" -> EnumMapping(EvidenceType)))
}