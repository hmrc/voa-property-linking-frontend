package useCaseSpecs.utils

import play.api.libs.json.Json

object SessionDocument {
  val sessionKey = ""

  implicit lazy val adf = Json.format[Address]
  implicit lazy val pf = Json.format[Property]
  implicit lazy val sdf = Json.format[SessionDocument]
}
case class SessionDocument(claimedProperty: Property)
case class Property(billingAuthorityReference: String, address: Address, bulkClass: String, isBank: Boolean, canReceiveMail: Boolean)
case class Address(lines: Seq[String], postcode: String, canReceiveCorrespondence: Boolean)
