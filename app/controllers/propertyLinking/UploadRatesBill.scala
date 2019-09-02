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
import javax.inject.Inject

import models.{RatesBillFlag, RatesBillType, UploadEvidenceData}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import services.BusinessRatesAttachmentService
import session.{LinkingSessionRequest, WithLinkingSession}
import uk.gov.hmrc.play.HeaderCarrierConverter
import views.html.propertyLinking.uploadRatesBill

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
          businessRatesAttachmentService.persistSessionData(request.ses, sessionUploadData).map(x => Redirect(routes.Declaration.show(Some(true)).url))
        case _ =>
          BadRequest(uploadRatesBill(request.ses.submissionId, List("error.businessRatesAttachment.file.not.selected"), Map()))
      }
  }

  def removeRatesBill(fileReference: String) =  {
    removeFile(fileReference)((submissionId, errors, sessionData, _) => implicit request => Ok(uploadRatesBill(submissionId, errors, sessionData)))
  }
}
