/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Named}

import config.{Global, Wiring}
import connectors.EnvelopeConnector
import connectors.fileUpload.FileUploadConnector
import controllers._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.RequestHeader
import repositories.SessionRepo
import session.WithLinkingSession
import views.html.propertyLinking.uploadRatesBill

class UploadRatesBill @Inject()(override val fileUploader: FileUploadConnector,
                                override val envelopeConnector: EnvelopeConnector,
                                @Named("propertyLinkingSession") override val sessionRepository: SessionRepo,
                                override val withLinkingSession: WithLinkingSession)
  extends PropertyLinkingController with FileUploadHelpers {

  lazy val propertyLinks = Wiring().propertyLinkConnector

  def show(errorCode: Option[Int], errorMessage: Option[String]) = withLinkingSession { implicit request =>
    errorCode match {
      case Some(REQUEST_ENTITY_TOO_LARGE) => EntityTooLarge(uploadRatesBill(UploadRatesBillVM(fileTooLargeError, failureUrl)))
      case Some(NOT_FOUND) => NotFound(Global.notFoundTemplate)
      case Some(UNSUPPORTED_MEDIA_TYPE) => UnsupportedMediaType(uploadRatesBill(UploadRatesBillVM(invalidFileTypeError, failureUrl)))
      case _ => Ok(uploadRatesBill(UploadRatesBillVM(form, fileUploadUrl(routes.UploadRatesBill.show().absoluteURL()))))
    }
  }

  lazy val form = Form(single("ratesBill[]" -> text))
  lazy val fileTooLargeError = form.withError("ratesBill[]", "error.fileUpload.tooLarge")
  lazy val invalidFileTypeError = form.withError("ratesBill[]", "error.fileUpload.invalidFileType")

  private def failureUrl(implicit requestHeader: RequestHeader) = routes.UploadRatesBill.show().absoluteURL()
}

case class UploadRatesBillVM(form: Form[_], submissionUrl: String)