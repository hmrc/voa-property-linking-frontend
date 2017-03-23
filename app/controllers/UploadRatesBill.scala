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

package controllers

import javax.inject.Inject

import config.Wiring
import connectors.{EnvelopeConnector, FileInfo}
import connectors.fileUpload.FileUploadConnector
import models._
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.Messages

class UploadRatesBill @Inject()(override val fileUploader: FileUploadConnector, override val envelopeConnector: EnvelopeConnector)
  extends PropertyLinkingController with FileUploadHelpers {

  import UploadRatesBill._

  lazy val propertyLinks = Wiring().propertyLinkConnector
  lazy val withLinkingSession = Wiring().withLinkingSession
  lazy val linkingSession = Wiring().sessionRepository

  def show() = withLinkingSession { implicit request =>
    Ok(views.html.uploadRatesBill.show(UploadRatesBillVM(form)))
  }

  def submit() = withLinkingSession { implicit request =>
    val filePart = request.request.body.asMultipartFormData.flatMap(_.file("ratesBill[]").flatMap(x => if (x.filename.isEmpty) None else Some(x)))
    uploadIfNeeded(filePart) flatMap {
      case FileAccepted =>
        val fileInfo = FileInfo(filePart.fold("No File")(_.filename), RatesBillType.name)
        linkingSession.saveOrUpdate(request.ses.withLinkBasis(RatesBillFlag, Some(fileInfo))) map { _ =>
          Redirect(propertyLinking.routes.Declaration.show())
        }
      case FileMissing =>
        BadRequest(views.html.uploadRatesBill.show(
          UploadRatesBillVM(form.withError(
            FormError("ratesBill[]", Messages("uploadRatesBill.ratesBillMissing.error"))))
        ))
      case FileTooLarge =>
        BadRequest(views.html.uploadRatesBill.show(
          UploadRatesBillVM(form.withError(
            FormError("ratesBill[]", Messages("error.fileUpload.tooLarge"))))
        ))
      case InvalidFileType =>
        BadRequest(views.html.uploadRatesBill.show(UploadRatesBillVM(form.withError(FormError("ratesBill[]", Messages("error.fileUpload.invalidFileType"))))))
    }
  }
}

object UploadRatesBill {
  lazy val form = Form(single("ratesBill[]" -> text))
}

case class UploadRatesBillVM(form: Form[_])