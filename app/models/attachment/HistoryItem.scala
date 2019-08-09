package models.attachment

import java.time.Instant

import play.api.libs.json.{Json, OFormat}

case class HistoryItem(state: AttachmentState, timeStamp: Instant, details: Option[String] = None)

object HistoryItem {
  implicit val format: OFormat[HistoryItem] = Json.format
}
