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
import config.ApplicationConfig
import connectors.FileAttachmentFailed
import controllers._
import form.EnumMapping
import models.EvidenceType
import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.PropertyLinkEvidence
import models.upscan.UploadedFileDetails
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.single
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, Request, Result}
import services.BusinessRatesAttachmentService
import session.{LinkingSessionRequest, WithLinkingSession}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.frontend.controller.Utf8MimeTypes
import views.html.propertyLinking.uploadRatesBill

import scala.concurrent.Future

abstract class FileUploadController (
                             val authenticated: AuthenticatedAction,
                             val withLinkingSession: WithLinkingSession,
                             val businessRatesAttachmentService: BusinessRatesAttachmentService
                           )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with BaseController with Utf8MimeTypes {

  lazy val form = Form(single("evidenceType" -> EnumMapping(EvidenceType)))

  def removeFile(fileReference: String)(f: (String, List[String], Map[String, UploadedFileDetails], Form[_]) => LinkingSessionRequest[_] => Result) = withLinkingSession { implicit request =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    val updatedSessionData = request.ses.uploadEvidenceData.attachments.map(map => map - fileReference)

    for{
      - <- businessRatesAttachmentService.persistSessionData(request.ses, request.ses.uploadEvidenceData.copy( attachments = updatedSessionData))
    }yield f(request.ses.submissionId, List.empty, updatedSessionData.getOrElse(Map()), form)(request)
  }

  def initiate(): Action[JsValue] = authenticated.async(parse.json) { implicit request  =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    val initiateAttachmentRequest = request.body.as[InitiateAttachmentRequest].copy(destination = Some(PropertyLinkEvidence.destination))

    (for {
            initiateAttachmentResult <- businessRatesAttachmentService.initiateAttachmentUpload(initiateAttachmentRequest)(request, hc)
          } yield
            Ok(Json.toJson(initiateAttachmentResult))
        ).recover {
          case fileAttachmentFailed: FileAttachmentFailed =>
            BadRequest(Json.toJson(Messages("error.businessRatesAttachment.does.not.support.file.types")))
          case ex: Exception =>
            Logger.warn("FileAttachmentFailed Exception:", ex)
            InternalServerError("500 INTERNAL_SERVER_ERROR")
        }
  }
}
