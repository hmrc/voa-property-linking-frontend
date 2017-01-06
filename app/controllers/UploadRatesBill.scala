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
import connectors.fileUpload.FileUpload
import connectors.FileInfo
import models._
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.AnyContent
import play.api.mvc.MultipartFormData.FilePart
import session.LinkingSessionRequest

import scala.concurrent.Future

class UploadRatesBill @Inject()(val fileUploadConnector: FileUpload) extends PropertyLinkingController {

  import UploadRatesBill._

  lazy val propertyLinkConnector = Wiring().propertyLinkConnector
  lazy val ratesBillConnector = Wiring().ratesBillVerificationConnector
  lazy val sessionRepository = Wiring().sessionRepository
  lazy val withLinkingSession = Wiring().withLinkingSession
  lazy val fileSystemConnector = Wiring().fileSystemConnector

  def show() = withLinkingSession { implicit request =>
    Ok(views.html.uploadRatesBill.show(UploadRatesBillVM(form)))
  }

  def submit() = withLinkingSession { implicit request =>
    val filePart = request.request.body.asMultipartFormData.get.file("ratesBill[]").flatMap(x => if (x.filename.isEmpty) None else Some(x))
    uploadFile(filePart) flatMap {
      case RatesBillUploaded =>
        requestLink(filePart.map(_.filename).getOrElse("Filename")) map { _ => Redirect(routes.UploadRatesBill.ratesBillUploaded()) }
      case RatesBillMissing =>
        BadRequest(views.html.uploadRatesBill.show(
          UploadRatesBillVM(form.withError(
            FormError("""ratesBill[]""", Messages("uploadRatesBill.ratesBillMissing.error"))))
        ))
    }
  }

  private def uploadFile(file: Option[FilePart[TemporaryFile]])(implicit request: LinkingSessionRequest[_]) = {
    file.map(filepart => {
      val envId = request.ses.envelopeId
      val contentType = filepart.contentType.getOrElse("application/octet-stream")
      fileUploadConnector.uploadFile(envId, filepart.filename, contentType, filepart.ref.file) map (x =>
        RatesBillUploaded
        )
    }).getOrElse(Future.successful(RatesBillMissing))
  }

  private def requestLink(fileName: String)(implicit req: LinkingSessionRequest[AnyContent]) =
    propertyLinkConnector.linkToProperty(
      req.ses.claimedProperty, req.groupAccount.id, req.individualAccount.individualId,
      req.ses.declaration.getOrElse(throw new Exception("No declaration")),
      req.ses.submissionId, RatesBillFlag, Some(FileInfo(fileName, RatesBillType.name))
    )

  def ratesBillUploaded() = withLinkingSession { implicit request =>
    fileUploadConnector.closeEnvelope(request.ses.envelopeId).flatMap(_ =>
      sessionRepository.remove().map(_ =>
        Ok(views.html.linkingRequestSubmitted())
      )
    )
  }
}

object UploadRatesBill {
  lazy val form = Form(single("ratesBill[]" -> text))
}

case class UploadRatesBillVM(form: Form[_])

sealed trait RatesBillUploadResult

case object RatesBillUploaded extends RatesBillUploadResult

case object RatesBillMissing extends RatesBillUploadResult
