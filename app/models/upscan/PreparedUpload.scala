package models.upscan

import play.api.libs.json.{Json, OFormat}

case class PreparedUpload(reference: Reference, uploadRequest: UploadFormTemplate)

object PreparedUpload {
  implicit val uploadFormat: OFormat[PreparedUpload] = Json.format
}

case class FileMetadata(fileName: String, contentType: String){
  def toDisplayFileName = fileName.split("-").toList.drop(2).mkString("-")
}

object FileMetadata {
  implicit val fileMetadata: OFormat[FileMetadata] = Json.format
}

case class UploadedFileDetails(fileMetadata: FileMetadata, preparedUpload: PreparedUpload)

object UploadedFileDetails {
  implicit val uploadedFileDetails: OFormat[UploadedFileDetails] = Json.format
}
