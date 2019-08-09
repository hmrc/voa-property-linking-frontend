package models.upscan

import play.api.libs.json.{Json, OFormat}

case class UploadFormTemplate(href: String, fields: Map[String, String])

object UploadFormTemplate {
  implicit val format: OFormat[UploadFormTemplate] = Json.format
}
