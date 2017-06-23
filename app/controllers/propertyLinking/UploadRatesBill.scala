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

import config.Wiring
import connectors.EnvelopeConnector
import connectors.fileUpload.FileUploadConnector
import controllers._
import models._
import org.apache.commons.io.FilenameUtils
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.libs.Files
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{AnyContent, Request}
import repositories.SessionRepo
import session.WithLinkingSession

class UploadRatesBill @Inject()(override val fileUploader: FileUploadConnector,
                                override val envelopeConnector: EnvelopeConnector,
                                @Named("propertyLinkingSession") override val sessionRepository: SessionRepo,
                                override val withLinkingSession: WithLinkingSession)
  extends PropertyLinkingController with FileUploadHelpers {

  import UploadRatesBill._

  lazy val propertyLinks = Wiring().propertyLinkConnector

  def show() = withLinkingSession { implicit request =>
    Ok(views.html.propertyLinking.uploadRatesBill(UploadRatesBillVM(form)))
  }

  def submit() = withLinkingSession { implicit request =>
    val filePart = getFile("ratesBill[]")
    uploadIfNeeded(filePart) flatMap {
      case FileAccepted =>
        val fileInfo = FileInfo(filePart.fold("No File")(_.filename), RatesBillType.name)
        sessionRepository.saveOrUpdate[LinkingSession](request.ses.withLinkBasis(RatesBillFlag, Some(fileInfo))) map { _ =>
          Redirect(propertyLinking.routes.Declaration.show())
        }
      case FileMissing =>
        BadRequest(views.html.propertyLinking.uploadRatesBill(
          UploadRatesBillVM(form.withError(
            FormError("ratesBill[]", Messages("uploadRatesBill.ratesBillMissing.error"))))
        ))
      case FileTooLarge =>
        BadRequest(views.html.propertyLinking.uploadRatesBill(
          UploadRatesBillVM(form.withError(
            FormError("ratesBill[]", Messages("error.fileUpload.tooLarge"))))
        ))
      case InvalidFileType =>
        BadRequest(views.html.propertyLinking.uploadRatesBill(UploadRatesBillVM(form.withError(FormError("ratesBill[]", Messages("error.fileUpload.invalidFileType")))))) //scalastyle:ignore
    }
  }
}

object UploadRatesBill {
  lazy val form = Form(single("ratesBill[]" -> text))
}

case class UploadRatesBillVM(form: Form[_])