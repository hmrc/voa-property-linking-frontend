package models.email

import play.api.libs.json.Json

case class EmailRequest(to: String, templateId: String, parameters: Map[String, String])

object EmailRequest {

  implicit val format = Json.format[EmailRequest]

  def apply(to: String, personId: String) =
    this(to, "TEMPLATEID", Map("PersonId" -> personId))
}