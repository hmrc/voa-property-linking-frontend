/*
 * Copyright 2022 HM Revenue & Customs
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

package models.upscan

import play.api.libs.json.{Json, OFormat}

case class PreparedUpload(reference: Reference, uploadRequest: UploadFormTemplate)

object PreparedUpload {
  implicit val uploadFormat: OFormat[PreparedUpload] = Json.format
}

case class FileMetadata(fileName: String, contentType: String) {
  def toDisplayFileName = fileName.split("-").toList.drop(1).mkString("-")
}

object FileMetadata {
  implicit val fileMetadata: OFormat[FileMetadata] = Json.format
}

case class UploadedFileDetails(fileMetadata: FileMetadata, preparedUpload: PreparedUpload)

object UploadedFileDetails {
  implicit val uploadedFileDetails: OFormat[UploadedFileDetails] = Json.format
}
