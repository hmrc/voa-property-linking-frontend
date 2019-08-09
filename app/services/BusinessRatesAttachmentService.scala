package services

import javax.inject.Inject

import auditing.AuditingService
import connectors.BusinessRatesAttachmentConnector
import models.LinkingSession
import models.attachment.InitiateAttachmentRequest
import models.upscan.{FileMetadata, PreparedUpload, UploadedFileDetails}
import play.api.libs.json.Json
import repositories.SessionRepo
import session.LinkingSessionRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessRatesAttachmentService @Inject()(
                                                businessRatesAttachmentConnector: BusinessRatesAttachmentConnector,
                                                val sessionRepository: SessionRepo,
                                                auditingService: AuditingService
                                              ) {

  def initiateAttachmentUpload(initiateAttachmentRequest: InitiateAttachmentRequest)(implicit request: LinkingSessionRequest, hc: HeaderCarrier): Future[PreparedUpload] = {
    for {
      initiateAttachmentResult <- businessRatesAttachmentConnector.initiateAttachmentUpload(initiateAttachmentRequest)
      updatedSessionData = updateSessionData(request.ses.updateBillsFiles, initiateAttachmentRequest, initiateAttachmentResult)
      _ <- persistSessionData(request, Some(updatedSessionData))
    } yield {
        AuditingService.sendEvent("property link rates bill upload", Json.obj(
          "organisationId" -> request.organisationId,
          "individualId" -> request.individualAccount.individualId,
          "propertyLinkSubmissionId" -> request.ses.submissionId,
          "fileName" -> initiateAttachmentRequest.fileName
        ))
      initiateAttachmentResult
    }
  }

  def updateSessionData(sessionData: Option[Map[String, UploadedFileDetails]],
                        initiateAttachmentRequest: InitiateAttachmentRequest,
                        initiateAttachmentResult: PreparedUpload): Map[String, UploadedFileDetails] = {
     sessionData.getOrElse(Map()) +
      (initiateAttachmentResult.reference.value -> UploadedFileDetails(FileMetadata(initiateAttachmentRequest.fileName, initiateAttachmentRequest.mimeType), initiateAttachmentResult))
  }

  def persistSessionData(request: LinkingSessionRequest, updatedSessionData: Option[Map[String, UploadedFileDetails]]) = {
    sessionRepository.saveOrUpdate[LinkingSession](request.ses.copy(updateBillsFiles = updatedSessionData))
  }

  def submitFiles(submissionId: String, uploadedFilesData: Option[Map[String, UploadedFileDetails]])(implicit request: DataRequest[_], hc: HeaderCarrier): Future[List[Option[Attachment]]] = {
    Future.traverse(uploadedFilesData.getOrElse(Map()).keys){ uploadedFile =>
     // auditingService.auditChallengeMetaData(submissionId, request.path)
      businessRatesAttachmentConnector.submitFile(uploadedFile, submissionId)
    }.map(_.toList)
  }

}


