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
import config.ApplicationConfig
import connectors.fileUpload.FileUploadConnector
import controllers._
import exceptionhandler.ErrorHandler
import form.EnumMapping
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Call}
import session.{LinkingSessionRequest, WithLinkingSession}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import views.html.propertyLinking.uploadEvidence

import scala.concurrent.Future

class UploadEvidence @Inject()(
                                override val withLinkingSession: WithLinkingSession,
                                withCircuitBreaker: FileUploadCircuitBreaker,
                                errorHandler: ErrorHandler,
                                auditingService: AuditingService,
                                fileUploadConnector: FileUploadConnector
                              )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with FileUploadHelpers {

  override val successUrl: String = routes.UploadEvidence.fileUploaded().url

  def show(errorCode: Option[Int], errorMessage: Option[String]): Action[AnyContent] = withLinkingSession { implicit request =>
    withCircuitBreaker {
      errorCode match {
        case Some(REQUEST_ENTITY_TOO_LARGE) => Future.successful(EntityTooLarge(uploadEvidence(UploadEvidenceVM(fileTooLarge, submissionCall))))
        case Some(NOT_FOUND)                => Future.successful(errorHandler.notFound)
        case Some(UNSUPPORTED_MEDIA_TYPE)   => Future.successful(UnsupportedMediaType(uploadEvidence(UploadEvidenceVM(invalidFileType, submissionCall))))
        // this assumes BAD_REQUEST is caused by "Envelope does not allow zero length files, and submitted file has length 0"
        case Some(BAD_REQUEST)              => Future.successful(UnsupportedMediaType(uploadEvidence(UploadEvidenceVM(invalidFileType, submissionCall))))
        //if FUaaS repeatedly returns unexpected error codes e.g. 500s, trigger the circuit breaker
        case Some(err)                      =>
          throw new IllegalArgumentException(s"Unexpected response from FUaaS: $err; ${errorMessage.map(msg => s"error: $msg")}")
        case None                           =>
          val envelopeId = request.ses.envelopeId
          fileUploadConnector.getFileMetadata(envelopeId).map(fileMetaData =>
            auditingService.sendEvent("property link evidence upload", Json.obj(
              "organisationId" -> request.organisationId,
              "individualId" -> request.individualAccount.individualId,
              "propertyLinkSubmissionId" -> request.ses.submissionId,
              "fileName" -> fileMetaData.fileInfo.fold("")(_.name)).toString
            ))

          Future.successful(Ok(uploadEvidence(UploadEvidenceVM(form, submissionCall))))
      }
    } recover {
      case _: UnhealthyServiceException => ServiceUnavailable(views.html.errors.serviceUnavailable())
    }
  }

  def noEvidenceUploaded(): Action[AnyContent] = withLinkingSession { implicit request =>
    if (config.fileUploadEnabled) {
      Future.successful(Redirect(propertyLinking.routes.Declaration.show()))
    } else {
      Future.successful(Redirect(propertyLinking.routes.Declaration.show(Some(true))))
    }
  }

  lazy val form = Form(single("evidenceType" -> EnumMapping(EvidenceType)))
  lazy val fileTooLarge: Form[EvidenceType] = form.withError("evidence[]", "error.fileUpload.tooLarge")
  lazy val invalidFileType: Form[EvidenceType] = form.withError("evidence[]", "error.fileUpload.invalidFileType")

  private def submissionCall(implicit request: LinkingSessionRequest[_]): Call = if (config.fileUploadEnabled) {
    Call("POST", fileUploadUrl(routes.UploadEvidence.show().url))
  } else {
    Call("GET", routes.Declaration.show().url)
  }
}

case class UploadEvidenceVM(form: Form[_], call: Call)
