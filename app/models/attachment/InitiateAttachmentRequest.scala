package models.attachment

import ai.x.play.json.Jsonx
import play.api.libs.json.OFormat

case class InitiateAttachmentRequest(
      fileName: String,
      mimeType: String,
      destination: Option[String],
      data: Map[String, String] = Map()) {}

object InitiateAttachmentRequest {

  implicit val format: OFormat[InitiateAttachmentRequest] = Jsonx.formatCaseClassUseDefaults

}
