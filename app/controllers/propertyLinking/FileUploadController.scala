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
import connectors.FileAttachmentFailed
import controllers._
import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.PropertyLinkEvidence
import play.api.Logger
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, Request}
import services.BusinessRatesAttachmentService
import session.WithLinkingSession
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.frontend.controller.Utf8MimeTypes
import views.html.propertyLinking.uploadRatesBill

class FileUploadController (
                             val authenticated: AuthenticatedAction,
                             val withLinkingSession: WithLinkingSession,
                             val businessRatesAttachmentService: BusinessRatesAttachmentService
                           )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with BaseController with Utf8MimeTypes {

  def initiate(): Action[JsValue] = authenticated.async(parse.json) { implicit request  =>
    implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    val initiateAttachmentRequest = request.body.as[InitiateAttachmentRequest].copy(destination = Some(PropertyLinkEvidence.destination))

    (for {
            initiateAttachmentResult <- businessRatesAttachmentService.initiateAttachmentUpload(initiateAttachmentRequest)(request, hc)
          } yield
            Ok(Json.toJson(initiateAttachmentResult))
        ).recover {
          case fileAttachmentFailed: FileAttachmentFailed =>
            BadRequest(Json.toJson(Messages("error.businessRatesAttachment.does.not.support.file.types")))
          case ex: Exception =>
            Logger.warn("FileAttachmentFailed Exception:", ex)
            InternalServerError("500 INTERNAL_SERVER_ERROR")
        }
  }
}
