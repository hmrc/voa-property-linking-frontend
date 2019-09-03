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
import models._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc._
import services.BusinessRatesAttachmentService
import session.WithLinkingSession
import uk.gov.hmrc.play.HeaderCarrierConverter
import views.html.propertyLinking.uploadEvidence
import EvidenceType.form
import scala.concurrent.Future

class UploadPropertyEvidence @Inject()(override val authenticated: AuthenticatedAction, override val withLinkingSession: WithLinkingSession, override val businessRatesAttachmentService: BusinessRatesAttachmentService)(implicit messagesApi: MessagesApi, config: ApplicationConfig)
  extends FileUploadController(authenticated, withLinkingSession, businessRatesAttachmentService) {

  def show() = withLinkingSession { implicit request =>
    Ok(uploadEvidence(
      submissionId = request.ses.submissionId,
      errors = List.empty,
      uploadedFiles = request.ses.uploadEvidenceData.attachments.getOrElse(Map.empty),
      formEvidence = request.ses.evidenceType.map(x => form.fill(x)).getOrElse(form)
      ))
  }

  def continue() = withLinkingSession { implicit request =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    request.ses.uploadEvidenceData.attachments match {
      case Some(fileData) if fileData.size > 0 => {
        form.bindFromRequest().fold(
          errors =>
            BadRequest(uploadEvidence(request.ses.submissionId, List("error.businessRatesAttachment.evidence.not.selected"), request.ses.uploadEvidenceData.attachments.getOrElse(Map.empty), form)),
          formData => {
            val session = request.ses.copy(evidenceType = EvidenceType.fromName(formData.name))
            val sessionUploadData: UploadEvidenceData =
              session.uploadEvidenceData.copy(linkBasis = OtherEvidenceFlag, fileInfo = request.ses.uploadEvidenceData.fileInfo.map(x => x.copy(evidenceType = EvidenceType.fromName(formData.name).get)))
            businessRatesAttachmentService.persistSessionData(session, sessionUploadData).map(x => Redirect(routes.Declaration.show().url))
          })
      }
      case _ =>
        BadRequest(uploadEvidence(request.ses.submissionId, List("error.businessRatesAttachment.file.not.selected"), Map(), form))
    }
  }

  def removeEvidence(fileReference: String) =  {
    removeFile(fileReference)((submissionId, errors, sessionData, form) => implicit request => Ok(uploadEvidence(submissionId, errors, sessionData, form)))
  }
}
