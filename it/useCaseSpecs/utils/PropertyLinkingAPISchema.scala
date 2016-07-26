package useCaseSpecs.utils

import play.api.libs.json.Json

object LinkToProperty {
  implicit lazy val cap = Json.format[CapacityDeclaration]
  implicit lazy val fmt = Json.format[LinkToProperty]
}

case class LinkToProperty(capacityDeclaration: CapacityDeclaration)
case class CapacityDeclaration(capacity: String, fromDate: String, toDate: Option[String])
