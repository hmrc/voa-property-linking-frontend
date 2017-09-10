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

package connectors.fileUpload

import java.io.File
import javax.inject.Inject

import akka.stream.scaladsl._
import com.google.inject.ImplementedBy
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

case class FileMetadata(linkBasis: LinkBasis, fileInfo: Option[FileInfo])

object FileMetadata {
  implicit val reads: Reads[FileMetadata] = (
      (__ \ "files" \ 0 \ "name").readNullable[String] and
      (__ \ "files" \ 0 \ "metadata" \ "evidenceType" \ 0).readNullable[EvidenceType]
  ) { FileMetadata.apply }

  def apply: (Option[String], Option[EvidenceType]) => FileMetadata = {
    case (name, evidence) if name.isEmpty || evidence.isEmpty /* "I have no evidence" route */ => FileMetadata(NoEvidenceFlag, None)
    case (Some(name), _) if name.isEmpty /* FUaaS doesn't prevent users from uploading no files! */ => FileMetadata(NoEvidenceFlag, None)
    case (Some(name), Some(RatesBillType)) => FileMetadata(RatesBillFlag, Some(FileInfo(name, RatesBillType)))
    case (Some(name), Some(evidence)) => FileMetadata(OtherEvidenceFlag, Some(FileInfo(name, evidence)))
  }
}

@ImplementedBy(classOf[FileUploadConnector])
trait FileUpload {
  def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier): Future[Unit]
  def getFileMetadata(envelopeId: String)(implicit hc: HeaderCarrier): Future[FileMetadata]
}

class FileUploadConnector @Inject()(val http: WSHttp)(implicit ec: ExecutionContext) extends FileUpload with ServicesConfig {

  def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"${baseUrl("file-upload-frontend")}/file-upload/upload/envelopes/$envelopeId/files/$fileName"
    http.buildRequest(url)
      .withHeaders(("X-Requested-With", "VOA_CCA"))
      .post(Source(FilePart(fileName, fileName, Option(contentType), FileIO.fromPath(file.toPath)) :: List()))
      .map(_ => Unit)
  }

  def getFileMetadata(envelopeId: String)(implicit hc: HeaderCarrier): Future[FileMetadata] = {
    http.GET[JsValue](s"${baseUrl("file-upload-backend")}/file-upload/envelopes/$envelopeId") map {
      _.validate[FileMetadata] match {
        case JsSuccess(data, _) => data
        case _ => FileMetadata(NoEvidenceFlag, None)
      }
    }
  }
}
