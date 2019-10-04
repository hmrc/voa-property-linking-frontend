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

package services

import actions.BasicAuthenticatedRequest
import auditing.AuditingService
import connectors.attachments.BusinessRatesAttachmentConnector
import javax.inject.{Inject, Named}
import models._
import models.attachment._
import models.upscan.{FileMetadata, PreparedUpload, UploadedFileDetails}
import play.api.Logger
import play.api.libs.json.Json
import repositories.SessionRepo
import session.LinkingSessionRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesAttachmentService @Inject()(
                                                businessRatesAttachmentConnector: BusinessRatesAttachmentConnector,
                                                @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                                                auditingService: AuditingService
                                              )(implicit executionContext: ExecutionContext) {

  def initiateAttachmentUpload(initiateAttachmentRequest: InitiateAttachmentPayload)(implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier): Future[PreparedUpload] = {
    for {
      linkSession               <- getSessionData()
      initiateAttachmentResult  <- businessRatesAttachmentConnector.initiateAttachmentUpload(initiateAttachmentRequest)
      updatedSessionData        = updateSessionData(linkSession.uploadEvidenceData, initiateAttachmentRequest, initiateAttachmentResult)
      _                         <- persistSessionData(linkSession, updatedSessionData)
    } yield {
      auditingService.sendEvent("property link rates bill upload", Json.obj(
          "organisationId" -> request.organisationId,
          "individualId" -> request.individualAccount.individualId,
          "propertyLinkSubmissionId" -> linkSession.submissionId,
          "fileName" -> initiateAttachmentRequest.fileName
        ))
      initiateAttachmentResult
    }
  }

  def updateSessionData(sessionUploadEvidenceData: UploadEvidenceData,
                        initiateAttachmentRequest: InitiateAttachmentPayload,
                        initiateAttachmentResult: PreparedUpload, linkBasis: LinkBasis = NoEvidenceFlag, evidenceType: EvidenceType = RatesBillType): UploadEvidenceData = {
      sessionUploadEvidenceData.copy(linkBasis = linkBasis,
        fileInfo = Some(FileInfo(initiateAttachmentRequest.fileName, evidenceType)),
        attachments = Some(Map((initiateAttachmentResult.reference.value -> UploadedFileDetails(FileMetadata(initiateAttachmentRequest.fileName, initiateAttachmentRequest.mimeType), initiateAttachmentResult))))
      )
  }

  def persistSessionData(linkingSession: LinkingSession, updatedSessionData: UploadEvidenceData)(implicit hc: HeaderCarrier): Future[Unit] = {
    sessionRepository.saveOrUpdate[LinkingSession](linkingSession.copy( uploadEvidenceData = updatedSessionData))
  }

  def getSessionData()(implicit hc: HeaderCarrier) = {
    sessionRepository.get[LinkingSession] map {
      case Some(session) => session
      case None =>
        Logger.warn(s"Invalid session data")
        throw new IllegalStateException("Invalid Session Data")
    }
  }

  //TODO the uploadedFilesData should not be optional.
  def submitFiles(submissionId: String, uploadedFilesData: Option[Map[String, UploadedFileDetails]])(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): Future[List[Option[Attachment]]] = {
    Future.traverse(uploadedFilesData.getOrElse(Map()).keys){ uploadedFile =>

      auditingService.sendEvent(
        auditType = "property link evidence upload",
        obj = Json.obj(
          "ggGroupId" -> request.groupAccount.groupId,
          "ggExternalId" -> request.individualAccount.externalId,
          "propertyLinkSubmissionId" -> request.ses.submissionId)
      )

      businessRatesAttachmentConnector.submitFile(uploadedFile, submissionId)
    }.map(_.toList)
  }

}


