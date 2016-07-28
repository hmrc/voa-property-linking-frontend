package useCaseSpecs.utils

import play.api.libs.json.Json

object LinkToProperty {
  implicit lazy val cap = Json.format[CapacityDeclaration]
  implicit lazy val fmt = Json.format[LinkToProperty]
}

object LinkedProperties {
  implicit lazy val plf = Json.format[PropertyLink]
  implicit lazy val lps = Json.format[LinkedProperties]
}

case class LinkToProperty(capacityDeclaration: CapacityDeclaration)
case class CapacityDeclaration(capacity: String, fromDate: String, toDate: Option[String])

case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PropertyLink])
case class PropertyLink(name: String, billingAuthorityReference: String, capacity: String, linkedDate: String)
