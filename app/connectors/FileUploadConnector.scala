/*
 * Copyright 2016 HM Revenue & Customs
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

package connectors

import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import serialization.JsonFormats.uploadedFileFormat
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

class FileUploadConnector(http: HttpGet)(implicit ec: ExecutionContext) extends ServicesConfig {
  private lazy val base = s"${baseUrl("file-uploads")}/file-uploads"

  def retrieveFile(accountId: String, sessionId: String, key: String, prototypeOnlyFile: Option[FilePart[TemporaryFile]])(implicit hc: HeaderCarrier): Future[Option[UploadedFile]] =
    http.GET[Option[UploadedFile]](s"$base/$accountId/$sessionId/$key")
        .recover { case _ => prototypeOnlyFile.map(p => UploadedFile(p.filename, "")) } // TODO - remove once backend exists and plugged in
}

case class UploadedFile(name: String, content: String)
