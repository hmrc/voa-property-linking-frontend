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
import config.Wiring
import connectors.EnvelopeConnector
import play.api.libs.json.{Format, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

case class EnvelopeMetadata(submissionId: String, personId: Long)

object EnvelopeMetadata {
  implicit val format: Format[EnvelopeMetadata] = Json.format[EnvelopeMetadata]
}

@ImplementedBy(classOf[FileUploadConnector])
trait FileUpload {
  def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier): Future[Unit]
}

class FileUploadConnector @Inject()(val ws: WSClient, val envelopeConnector: EnvelopeConnector)(implicit ec: ExecutionContext)
  extends FileUpload with ServicesConfig with JsonHttpReads {

  lazy val http = Wiring().http

  def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"${baseUrl("file-upload-frontend")}/file-upload/upload/envelopes/$envelopeId/files/$fileName"
    ws.url(url)
      .withHeaders(("X-Requested-With", "VOA_CCA"))
      .post(Source(FilePart(fileName, fileName, Option(contentType), FileIO.fromFile(file)) :: List()))
      .map(_ => Unit)
  }

}
