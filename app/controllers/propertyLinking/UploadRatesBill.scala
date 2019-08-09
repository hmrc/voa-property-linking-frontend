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

import auditing.AuditingService
import config.{ApplicationConfig, Global}
import connectors.FileAttachmentFailed
import connectors.fileUpload.FileUploadConnector
import controllers._
import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.ChallengeCaseEvidence
import models.upscan.UploadedFileDetails
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Call}
import repositories.SessionRepo
import services.BusinessRatesAttachmentService
import session.{LinkingSessionRequest, WithLinkingSession}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.frontend.controller.Utf8MimeTypes
import views.html.propertyLinking.uploadRatesBill

class UploadRatesBill @Inject()(val withLinkingSession: WithLinkingSession, val sessionRepository: SessionRepo, businessRatesAttachmentService: BusinessRatesAttachmentService)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with BaseController with Utf8MimeTypes {

  def show(errorCode: Option[Int], errorMessage: Option[String]) = withLinkingSession { implicit request =>
    withCircuitBreaker {
      errorCode match {
        case Some(REQUEST_ENTITY_TOO_LARGE) => EntityTooLarge(uploadRatesBill(UploadRatesBillVM(fileTooLargeError, submissionCall)))
        case Some(NOT_FOUND) => NotFound(Global.notFoundTemplate)
        case Some(UNSUPPORTED_MEDIA_TYPE) => UnsupportedMediaType(uploadRatesBill(UploadRatesBillVM(invalidFileTypeError, submissionCall)))
        // this assumes BAD_REQUEST is caused by "Envelope does not allow zero length files, and submitted file has length 0"
        case Some(BAD_REQUEST) => UnsupportedMediaType(uploadRatesBill(UploadRatesBillVM(invalidFileTypeError, submissionCall)))
        //if FUaaS repeatedly returns unexpected error codes e.g. 500s, trigger the circuit breaker
        case Some(err) => throw new IllegalArgumentException(s"Unexpected response from FUaaS: $err; ${errorMessage.map(msg => s"error: $msg")}")
        case None => {
          val envelopeId = request.ses.envelopeId
          fileUploadConnector.getFileMetadata(envelopeId).map(fileMetaData =>
            AuditingService.sendEvent("property link rates bill upload", Json.obj(
              "organisationId" -> request.organisationId,
              "individualId" -> request.individualAccount.individualId,
              "propertyLinkSubmissionId" -> request.ses.submissionId,
              "fileName" -> fileMetaData.fileInfo.fold("")(_.name)).toString
            ))
          Ok(uploadRatesBill(UploadRatesBillVM(form, submissionCall)))
        }
      }
    } recover {
      case _: UnhealthyServiceException => ServiceUnavailable(views.html.errors.serviceUnavailable())
    }
  }

  def initiate(): Action[JsValue] = withLinkingSession { implicit request =>
    withJsonBody[InitiateAttachmentRequest] { initiateRequest =>{
      Logger.debug(s"Processing request: [$initiateRequest].")
      val submissionId: String  = request.ses.submissionId
      (

        for {
          sessionData <- request.ses
          initiateAttachmentResult <- businessRatesAttachmentService.initiateAttachmentUpload(initiateRequest.copy(destination = Some(ChallengeCaseEvidence.destination)))
        } yield
          Ok(Json.toJson(initiateAttachmentResult))
        ) recover {
        case fileAttachmentFailed: FileAttachmentFailed =>
          Logger.warn("FileAttachmentFailed Bad Request Exception:" + fileAttachmentFailed.errorMessage)
          BadRequest(uploadRatesBill(submissionId, List("error.businessRatesAttachment.does.not.support.file.types"), Map()))
        case ex: Exception =>
          Logger.warn("FileAttachmentFailed Exception:" + ex.getMessage)
          InternalServerError("500 INTERNAL_SERVER_ERROR")
      }
    }
    }
  }

}

case class UploadRatesBillVM(form: Form[_], call: Call)