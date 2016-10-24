package useCaseSpecs.utils

import play.api.libs.json.Json

object CapacityDeclaration {
  implicit lazy val capDecFmt = Json.format[CapacityDeclaration]
}

object PropertyLink {
  implicit lazy val plf = Json.format[PropertyLink]
}
object LinkedProperties {
  implicit lazy val lps = Json.format[LinkedProperties]
}

object PropertyRepresentation {
  implicit lazy val  fmt = Json.format[PropertyRepresentation]
}
object Account {
  implicit val accountFmt = Json.format[Account]
}




case class CapacityDeclaration(capacity: String, fromDate: String, toDate: Option[String])

case class PropertyLink(uarn: String, userId: String, capacityDeclaration: CapacityDeclaration,
                        linkedDate: String, assessmentYears: Seq[Int], pending: Boolean, requestFlag: String)
case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PropertyLink])
case class PropertyRepresentation(representationId: String, agentId: String, userId: String, uarn: String,
                                  canCheck: Boolean, canChallenge: Boolean, pending: Boolean)
case class Account(companyName: String, isAgent:Boolean)
