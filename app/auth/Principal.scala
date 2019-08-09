package auth

import play.api.libs.json.{Json, OFormat}

case class Principal(externalId: String, groupId: String)

object Principal {
  implicit val format: OFormat[Principal] = Json.format
}
