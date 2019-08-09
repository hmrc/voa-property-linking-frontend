package models.attachment

import java.time.Instant
import java.util.UUID

import auth.Principal
import models.upscan.{PreparedUpload, ScanResult}
import play.api.libs.json.{Json, OFormat}

case class Attachment(
      _id: UUID,
      initiatedAt: Instant,
      fileName: String,
      mimeType: String,
      destination: String,
      data: Map[String, String],
      state: AttachmentState,
      history: List[HistoryItem],
      scanResult: Option[ScanResult],
      initiateResult: Option[PreparedUpload],
      principal: Principal)

object Attachment {
  implicit val format: OFormat[Attachment] = Json.format
}
