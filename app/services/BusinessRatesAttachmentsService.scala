/*
 * Copyright 2022 HM Revenue & Customs
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

import actions.propertylinking.requests.LinkingSessionRequest
import auditing.AuditingService
import cats.data.EitherT
import connectors.attachments.BusinessRatesAttachmentsConnector
import javax.inject.{Inject, Named}
import models._
import models.attachment.{Attachment, _}
import models.upscan.{FileMetadata, PreparedUpload, UploadedFileDetails}
import play.api.libs.json.Json
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.exceptions.attachments._
import utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesAttachmentsService @Inject()(
      businessRatesAttachmentsConnector: BusinessRatesAttachmentsConnector,
      @Named("propertyLinkingSession") sessionRepository: SessionRepo,
      auditingService: AuditingService
)(implicit executionContext: ExecutionContext)
    extends Cats {

  def initiateAttachmentUpload(initiateAttachmentRequest: InitiateAttachmentPayload, evidenceType: EvidenceType)(
        implicit request: LinkingSessionRequest[_],
        hc: HeaderCarrier): Future[PreparedUpload] =
    for {
      initiateAttachmentResult <- businessRatesAttachmentsConnector.initiateAttachmentUpload(initiateAttachmentRequest)
      updatedSessionData = updateSessionData(
        sessionUploadEvidenceData = request.ses.uploadEvidenceData,
        initiateAttachmentRequest = initiateAttachmentRequest,
        initiateAttachmentResult = initiateAttachmentResult,
        evidenceType = evidenceType
      )
      _ <- persistSessionData(request.ses, updatedSessionData)
    } yield {
      auditingService.sendEvent(
        "property link rates bill upload",
        Json.obj(
          "organisationId"           -> request.organisationId,
          "individualId"             -> request.individualAccount.individualId,
          "propertyLinkSubmissionId" -> request.ses.submissionId,
          "fileName"                 -> initiateAttachmentRequest.fileName
        )
      )
      initiateAttachmentResult
    }

  private def updateSessionData(
        sessionUploadEvidenceData: UploadEvidenceData,
        initiateAttachmentRequest: InitiateAttachmentPayload,
        initiateAttachmentResult: PreparedUpload,
        linkBasis: LinkBasis = NoEvidenceFlag,
        evidenceType: EvidenceType): UploadEvidenceData =
    sessionUploadEvidenceData.copy(
      linkBasis = linkBasis,
      fileInfo = Some(CompleteFileInfo(initiateAttachmentRequest.fileName, evidenceType)),
      attachments = Some(
        Map(
          initiateAttachmentResult.reference.value -> UploadedFileDetails(
            FileMetadata(initiateAttachmentRequest.fileName, initiateAttachmentRequest.mimeType),
            initiateAttachmentResult)))
    )

  def persistSessionData(linkingSession: LinkingSession)(implicit hc: HeaderCarrier): Future[Unit] =
    sessionRepository.saveOrUpdate[LinkingSession](linkingSession)

  def persistSessionData(linkingSession: LinkingSession, updatedSessionData: UploadEvidenceData)(
        implicit hc: HeaderCarrier): Future[Unit] =
    sessionRepository.saveOrUpdate[LinkingSession](linkingSession.copy(uploadEvidenceData = updatedSessionData))

  def submit(
        submissionId: String,
        nonEmptyReferences: List[String],
        retryCount: Int = 0
  )(
        implicit request: LinkingSessionRequest[_],
        hc: HeaderCarrier): EitherT[Future, AttachmentException, List[Attachment]] =
    EitherT
      .fromEither[Future](Either.cond(nonEmptyReferences.nonEmpty, nonEmptyReferences, MissingRequiredNumberOfFiles))
      .semiflatMap(Future.traverse(_)(r => getAttachment(r).map(r -> _)))
      .subflatMap(attachments => {
        Either.cond(
          attachments
            .count { case (_, attachment) => isAttachmentsHasMovedToUploadStatus(attachment) } != nonEmptyReferences.size,
          attachments,
          AllFilesAreAlreadyUploaded(attachments.map(_._2))
        )

      })
      .subflatMap { attachments =>
        val (uploaded, notuploaded) =
          attachments.partition(attachment => isAttachmentsHasMovedToUploadStatus(attachment._2))
        Either.cond(uploaded.isEmpty, attachments, SomeFilesAreAlreadyUploaded(notuploaded.map(_._1)))
      }
      .map(result => result.filter(_._2.state == MetadataPending))
      .subflatMap(filtered =>
        Either.cond(filtered.size == nonEmptyReferences.size, filtered.map(_._1), NotAllFilesReadyToUpload))
      .semiflatMap(references => Future.traverse(references)(patchMetadata(submissionId, _)))
      .leftFlatMap {
        case AllFilesAreAlreadyUploaded(attachments) => EitherT.rightT(attachments)
        case error @ SomeFilesAreAlreadyUploaded(references) if (retryCount < 5) =>
          submit(submissionId, references, retryCount + 1)
        case error => EitherT.leftT(error)
      }

  def getAttachment(reference: String)(implicit hc: HeaderCarrier): Future[Attachment] =
    businessRatesAttachmentsConnector.getAttachment(reference)

  def patchMetadata(submissionId: String, reference: String)(
        implicit request: LinkingSessionRequest[_],
        hc: HeaderCarrier): Future[Attachment] = {
    auditingService.sendEvent(
      auditType = "property link evidence upload",
      obj = Json.obj(
        "ggGroupId"                -> request.organisationAccount.groupId,
        "ggExternalId"             -> request.individualAccount.externalId,
        "propertyLinkSubmissionId" -> submissionId)
    )

    businessRatesAttachmentsConnector.submitFile(reference, submissionId)
  }

  private def isAttachmentsHasMovedToUploadStatus(attachment: Attachment): Boolean =
    List(
      UploadPending,
      Uploading,
      UploadAttachmentFailed,
      UploadAttachmentComplete,
      UploadingScanResults,
      UploadScanResultsFailed,
      UploadScanResultsComplete
    ).contains(attachment.state)

}
