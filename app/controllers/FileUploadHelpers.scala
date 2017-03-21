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

import java.net.URLEncoder

import config.ApplicationConfig
import connectors.{EnvelopeConnector, FileInfo}
import connectors.fileUpload.FileUpload
import connectors.propertyLinking.PropertyLinkConnector
import models.{LinkBasis, NoEvidenceFlag}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.AnyContent
import play.api.mvc.MultipartFormData.FilePart
import session.{LinkingSessionRepository, LinkingSessionRequest, WithLinkingSession}

import scala.concurrent.Future

trait FileUploadHelpers {
  self: PropertyLinkingController =>

  val fileUploader: FileUpload
  val envelopeConnector: EnvelopeConnector
  val propertyLinks: PropertyLinkConnector
  val withLinkingSession: WithLinkingSession
  val linkingSession: LinkingSessionRepository

  val maxFileSize = 10485760 //10MB

  protected def uploadIfNeeded(filePart: Option[FilePart[TemporaryFile]])
                              (implicit request: LinkingSessionRequest[AnyContent]): Future[FileUploadResult] = {
    filePart match {
      case Some(part) if part.ref.file.length > maxFileSize => FileTooLarge
      case Some(FilePart(_, filename, Some(mimetype), TemporaryFile(file))) if ApplicationConfig.allowedMimeTypes.contains(mimetype) =>
        for {
          _ <- fileUploader.uploadFile(request.ses.envelopeId, encode(filename), mimetype, file)
        } yield {
          FileAccepted
        }
      case Some(part) /* wrong mimetype */ => InvalidFileType
      case None => FileMissing
    }
  }

  def fileUploaded() = withLinkingSession { implicit request =>
    Redirect(propertyLinking.routes.Declaration.show())
  }

  private def encode(fileName: String)(implicit request: LinkingSessionRequest[AnyContent]) = {
    URLEncoder.encode(fileName, "UTF-8")
  }
}

sealed trait FileUploadResult

case object FileAccepted extends FileUploadResult

case object FileMissing extends FileUploadResult

case object FileTooLarge extends FileUploadResult

case object InvalidFileType extends FileUploadResult