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

import javax.inject.{Inject, Named}

import actions.BasicAuthenticatedRequest
import auditing.AuditingService
import config.Global
import connectors.{BusinessRatesAttachmentConnector, InvalidGGSession}
import models._
import models.attachment.InitiateAttachmentRequest
import models.upscan.{FileMetadata, PreparedUpload, UploadedFileDetails}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results._
import repositories.SessionRepo
import session.LinkingSessionRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import models.attachment._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessRatesAttachmentService @Inject()(
                                                businessRatesAttachmentConnector: BusinessRatesAttachmentConnector,
                                                @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
                                                auditingService: AuditingService
                                              ) {
  def initiateAttachmentUpload(initiateAttachmentRequest: InitiateAttachmentRequest)(implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier): Future[PreparedUpload] = {
    for {
      linkSession  <- getSessionData()
      initiateAttachmentResult <- businessRatesAttachmentConnector.initiateAttachmentUpload(initiateAttachmentRequest)
      updatedSessionData = updateSessionData(linkSession.uploadEvidenceData, initiateAttachmentRequest, initiateAttachmentResult)
      _ <- persistSessionData(linkSession, updatedSessionData)
    } yield {
        AuditingService.sendEvent("property link rates bill upload", Json.obj(
          "organisationId" -> request.organisationId,
          "individualId" -> request.individualAccount.individualId,
          "propertyLinkSubmissionId" -> linkSession.submissionId,
          "fileName" -> initiateAttachmentRequest.fileName
        ))
      initiateAttachmentResult
    }
  }


  def updateSessionData(sessionUploadEvidenceData: UploadEvidenceData,
                        initiateAttachmentRequest: InitiateAttachmentRequest,
                        initiateAttachmentResult: PreparedUpload, linkBasis: LinkBasis = NoEvidenceFlag, evidenceType: EvidenceType = RatesBillType): UploadEvidenceData = {
      sessionUploadEvidenceData.copy(linkBasis = linkBasis,
        fileInfo = Some(FileInfo(initiateAttachmentRequest.fileName, evidenceType)),
        attachments = Some(Map((initiateAttachmentResult.reference.value -> UploadedFileDetails(FileMetadata(initiateAttachmentRequest.fileName, initiateAttachmentRequest.mimeType), initiateAttachmentResult))))
      )
  }




  def persistSessionData(linkingSession: LinkingSession, updatedSessionData: UploadEvidenceData)(implicit hc: HeaderCarrier) = {
    sessionRepository.saveOrUpdate[LinkingSession](linkingSession.copy( uploadEvidenceData= updatedSessionData))
  }

  def getSessionData()(implicit hc: HeaderCarrier) = {
    sessionRepository.get[LinkingSession] map {
      case Some(session) => session
      case None =>
        Logger.warn(s"Invalid session data")
        throw new IllegalStateException("Invalid Session Data")
    }
  }


  def submitFiles(submissionId: String, uploadedFilesData: Option[Map[String, UploadedFileDetails]])(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): Future[List[Option[Attachment]]] = {
    Future.traverse(uploadedFilesData.getOrElse(Map()).keys){ uploadedFile =>

      AuditingService.sendEvent("property link evidence upload", Json.obj(
        "ggGroupId" -> request.groupAccount.groupId,
        "ggExternalId" -> request.individualAccount.externalId,
        "propertyLinkSubmissionId" -> request.ses.submissionId))
      businessRatesAttachmentConnector.submitFile(uploadedFile, submissionId)
    }.map(_.toList)
  }

}


