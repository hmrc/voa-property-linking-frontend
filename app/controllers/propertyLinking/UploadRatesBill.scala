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
import models.{FileInfo, RatesBillFlag, RatesBillType, UploadEvidenceData}
import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.PropertyLinkEvidence
import play.api.Logger
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc.{Action, Request}
import services.BusinessRatesAttachmentService
import session.WithLinkingSession
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.frontend.controller.Utf8MimeTypes
import views.html.propertyLinking.uploadRatesBill

import scala.concurrent.{ExecutionContext, Future}

class UploadRatesBill @Inject()(override val authenticated: AuthenticatedAction, override val withLinkingSession: WithLinkingSession, override val businessRatesAttachmentService: BusinessRatesAttachmentService)(implicit messagesApi: MessagesApi, config: ApplicationConfig)
  extends FileUploadController(authenticated, withLinkingSession, businessRatesAttachmentService) {

  def show() = withLinkingSession { implicit request =>
    Ok(
      uploadRatesBill(request.ses.submissionId, List.empty, request.ses.uploadEvidenceData.attachments.getOrElse(Map.empty)))}

  def continue() = withLinkingSession { implicit request =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    request.ses.uploadEvidenceData.attachments match {
        case Some(fileData) if fileData.size > 0 =>
          val sessionUploadData: UploadEvidenceData =
            request.ses.uploadEvidenceData.copy(linkBasis = RatesBillFlag, fileInfo = request.ses.uploadEvidenceData.fileInfo.map(x => x.copy(evidenceType = RatesBillType)))
          businessRatesAttachmentService.persistSessionData(request.ses, sessionUploadData).map(x => Redirect(routes.Declaration.show().url))
        case _ =>
          BadRequest(uploadRatesBill(request.ses.submissionId, List("error.businessRatesAttachment.file.not.selected"), Map()))
      }
  }

  def removeFile(fileReference: String) = withLinkingSession { implicit request =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    val updatedSessionData = request.ses.uploadEvidenceData.attachments.map(map => map - fileReference)

    for{
      - <- businessRatesAttachmentService.persistSessionData(request.ses, request.ses.uploadEvidenceData.copy( attachments = updatedSessionData))
    }yield (Ok(uploadRatesBill(request.ses.submissionId, List.empty, updatedSessionData.getOrElse(Map()))))
  }

}
