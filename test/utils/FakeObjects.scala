package utils

import java.time.{Instant, LocalDate}
import java.util.UUID

import auth.Principal
import models.attachment._
import models.upscan._


trait FakeObjects {

  val preparedUpload = PreparedUpload(Reference("1862956069192540"), UploadFormTemplate("http://localhost/upscan", Map()))
  private val FILE_REFERENCE: String = "1862956069192540"
  val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")
  val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)
  val fileUpscanMetaData = Map(FILE_REFERENCE -> uploadedFileDetails)
  val attachment = Attachment(UUID.randomUUID(), Instant.now(), "fileName", "image/jpeg", "DESTINATION", Map(), Initiated, List(), None, None, Principal("externalId", "groupId"))


}