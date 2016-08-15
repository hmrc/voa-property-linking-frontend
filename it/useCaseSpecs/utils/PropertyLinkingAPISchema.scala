package useCaseSpecs.utils

import play.api.libs.json.Json

object LinkToProperty {
  implicit lazy val cap = Json.format[CapacityDeclaration]
  implicit lazy val fmt = Json.format[LinkToProperty]
}

object LinkedProperties {
  implicit lazy val plf = Json.format[PropertyLink]
  implicit lazy val pplf = Json.format[PendingPropertyLink]
  implicit lazy val lps = Json.format[LinkedProperties]
}

case class LinkToProperty(capacityDeclaration: CapacityDeclaration)
case class CapacityDeclaration(capacity: String, fromDate: String, toDate: Option[String])

case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PendingPropertyLink])
case class PropertyLink(name: String, uarn: String, billingAuthorityReference: String, capacity: String, linkedDate: String, assessmentYears: Seq[Int])
case class PendingPropertyLink(name: String, uarn: String, billingAuthorityReference: String, capacity: String, linkedDate: String)
case class PropertyRepresentation(representationId: String, agentId: String, userId: String, uarn: String,
                                  canCheck: Boolean, canChallenge: Boolean, pending: Boolean)
