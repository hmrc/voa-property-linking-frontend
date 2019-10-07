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

import actions.propertylinking.LinkingSessionRequest
import auditing.AuditingService
import cats.data.EitherT
import connectors.attachments.BusinessRatesAttachmentConnector
import javax.inject.{Inject, Named}
import models._
import models.attachment._
import models.upscan.{FileMetadata, PreparedUpload, UploadedFileDetails}
import play.api.libs.json.Json
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.propertylinking.exceptions.attachments.{AttachmentException, MissingRequiredNumberOfFiles, NotAllFilesReadyToUpload}
import utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesAttachmentService @Inject()(
                                                businessRatesAttachmentConnector: BusinessRatesAttachmentConnector,
                                                @Named("propertyLinkingSession") sessionRepository: SessionRepo,
                                                auditingService: AuditingService
                                              )(implicit executionContext: ExecutionContext) extends Cats {

  def initiateAttachmentUpload(initiateAttachmentRequest: InitiateAttachmentPayload)(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): Future[PreparedUpload] = {
    for {
      initiateAttachmentResult  <- businessRatesAttachmentConnector.initiateAttachmentUpload(initiateAttachmentRequest)
      updatedSessionData        = updateSessionData(request.ses.uploadEvidenceData, initiateAttachmentRequest, initiateAttachmentResult)
      _                         <- persistSessionData(request.ses, updatedSessionData)
    } yield {
      auditingService.sendEvent("property link rates bill upload", Json.obj(
          "organisationId" -> request.organisationId,
          "individualId" -> request.individualAccount.individualId,
          "propertyLinkSubmissionId" -> request.ses.submissionId,
          "fileName" -> initiateAttachmentRequest.fileName
        ))
      initiateAttachmentResult
    }
  }

  def updateSessionData(sessionUploadEvidenceData: UploadEvidenceData,
                        initiateAttachmentRequest: InitiateAttachmentPayload,
                        initiateAttachmentResult: PreparedUpload, linkBasis: LinkBasis = NoEvidenceFlag, evidenceType: EvidenceType = RatesBillType): UploadEvidenceData = {
      sessionUploadEvidenceData.copy(
        linkBasis = linkBasis,
        fileInfo = Some(FileInfo(initiateAttachmentRequest.fileName, evidenceType)),
        attachments = Some(Map(initiateAttachmentResult.reference.value -> UploadedFileDetails(FileMetadata(initiateAttachmentRequest.fileName, initiateAttachmentRequest.mimeType), initiateAttachmentResult)))
      )
  }

  def persistSessionData(linkingSession: LinkingSession, updatedSessionData: UploadEvidenceData)(implicit hc: HeaderCarrier): Future[Unit] = {
    sessionRepository.saveOrUpdate[LinkingSession](linkingSession.copy( uploadEvidenceData = updatedSessionData))
  }

  def submit(
              submissionId: String,
              nonEmptyReferences: List[String]
            )(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): EitherT[Future, AttachmentException, List[Attachment]] = {
    EitherT.fromEither[Future](Either.cond(nonEmptyReferences.nonEmpty, nonEmptyReferences, MissingRequiredNumberOfFiles))
      .semiflatMap(Future.traverse(_)(r => getAttachment(r).map(r -> _)))
      .map(result => result.filter(_._2.state == MetadataPending))
      .subflatMap(filtered => Either.cond(filtered.size == nonEmptyReferences.size, filtered.map(_._1), NotAllFilesReadyToUpload))
      .semiflatMap(references => Future.traverse(references)(patchMetadata(submissionId, _)))
  }

  def getAttachment(reference: String)(implicit hc: HeaderCarrier): Future[Attachment] =
    businessRatesAttachmentConnector.getAttachment(reference)

  def patchMetadata(submissionId: String, reference: String)(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): Future[Attachment] = {
      auditingService.sendEvent(
        auditType = "property link evidence upload",
        obj = Json.obj(
          "ggGroupId" -> request.groupAccount.groupId,
          "ggExternalId" -> request.individualAccount.externalId,
          "propertyLinkSubmissionId" -> submissionId)
      )

      businessRatesAttachmentConnector.submitFile(reference, submissionId)
    }

}


