package useCaseSpecs.utils

import play.api.libs.json.Json

object SessionDocument {
  val sessionKey = ""

  implicit val adf = Json.format[Address]
  implicit val pf = Json.format[Property]
  implicit val sdf = Json.format[SessionDocument]
}
case class SessionDocument(claimedProperty: Property)
case class Property(billingAuthorityReference: String, address: Address, bulkClass: String, isBank: Boolean, canReceiveMail: Boolean)
case class Address(lines: Seq[String], postcode: String, canReceiveCorrespondence: Boolean)
