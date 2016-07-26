package useCaseSpecs.utils

import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads, Writes}

object SessionDocument {
  val sessionKey = "sessiondocument"

  implicit lazy val dateReads = Reads.jodaDateReads("dd-MM-yyyy")
  implicit lazy val dateWrites = Writes.jodaDateWrites("dd-MM-yyyy")
  implicit lazy val ldf = Json.format[CapacityDeclaration]
  implicit lazy val adf = Json.format[Address]
  implicit lazy val pf = Json.format[Property]
  implicit lazy val sdf = Json.format[SessionDocument]
}

case class SessionDocument(claimedProperty: Property, declaration: Option[CapacityDeclaration] = None)
case class Property(billingAuthorityReference: String, address: Address, bulkClass: String, isBank: Boolean, canReceiveMail: Boolean)
case class Address(lines: Seq[String], postcode: String, canReceiveCorrespondence: Boolean)
case class CapacityDeclaration(capacity: String, fromDate: DateTime, toDate: Option[DateTime])
