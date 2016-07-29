package useCaseSpecs

import useCaseSpecs.utils._

class CapacityDeclaration extends FrontendTest {
  import TestData._

  "Given an interested person is logged in and has selected a self-certifiable property that exists to claim" - {
    implicit val sid: SessionID = sessionId
    implicit val aid: AccountID = accountId
    HTTP.stubPropertiesAPI(propertyToClaimBillingAuthorityRef, selfCertifiableProperty)

    "When they arrive at the declaration page" - {
      val page = Page.get(s"/property-linking/link-to-property/$propertyToClaimBillingAuthorityRef")

      "They can see the address of the property they wish to claim" in {
        page.mustContainText((selfCertifiableProperty.address.lines :+ selfCertifiableProperty.address.postcode).mkString(", "))
      }

      "And They are asked to provide:" - {

        "their relationship to the property" in {
          page.mustContainRadioSelect("capacity", Seq("occupier", "ownerlandlord", "previousOccupier"))
        }

        "the date they became an interested person of the selected property" in {
          page.mustContainDateSelect("fromDate")
        }

        "the date they ceased to be an interested person of the selected property" in {
          page.mustContainDateSelect("toDate")
        }
      }
    }

    "When they supply a valid relationship, start and end date" - {
      HTTP.stubKeystoreSession(SessionDocument(selfCertifiableProperty))
      val response = Page.postValid("/property-linking/link-to-property", validFormData:_*)

      "Their declaration is stored in the session" in {
        HTTP.verifyKeystoreSaved(
          SessionDocument(selfCertifiableProperty, Some(CapacityDeclaration(validRelationship, fromDate, Some(toDate))))
        )
      }

      "And they continue to the next step of the self-certification journey" in {
        response.header.status mustEqual 303
        response.header.headers("location") mustEqual "/property-linking/self-certify"
      }

      "But if a non-self-certifiable property had been chosen they would instead be asked to supply a rates bill" in {
        val sid: SessionID = java.util.UUID.randomUUID.toString
        HTTP.stubKeystoreSession(SessionDocument(nonSelfCertifiableProperty))(sid)
        val response = Page.postValid("/property-linking/link-to-property", validFormData:_*)(sid, "blah222")
        response.header.headers("location") mustEqual "/property-linking/supply-rates-bill"
      }
    }

    "When they do not supply a valid relationship, start or end date" - {
      val formData = Seq("capacity" -> invalidRelationship) ++ fromDateFields ++ toDateFields
      val page = Page.postInvalid("/property-linking/link-to-property", formData:_*)

      "An error summary is shown" in {
        page.mustContainSummaryErrors(("capacity", "What is your connection to the property?", "No value selected"))
      }

      "A field-level error is shown for each invalid field" in {
        page.mustContainFieldErrors("capacity" -> "No value selected")
      }
    }
  }

  object TestData {
    lazy val sessionId = java.util.UUID.randomUUID().toString
    lazy val propertyToClaimBillingAuthorityRef = "blahblahblooh"

    lazy val accountId = "asdfj2304rsdf"
    lazy val address = Address(Seq("123 somwhere333i", "a teeewonio", "une villagAArios"), "AA11 1AA", true)
    lazy val selfCertifiableProperty = Property(propertyToClaimBillingAuthorityRef, address, true, true)
    lazy val nonSelfCertifiableProperty = Property(propertyToClaimBillingAuthorityRef, address, false, true)
    lazy val validRelationship = "ownerlandlord"
    lazy val invalidRelationship = "re44wo"
    lazy val fromDate = "23-12-2001"
    lazy val toDate = "15-01-2003"
    lazy val fromDateFields = Seq("fromDate.day" -> "23", "fromDate.month" -> "12", "fromDate.year" -> "2001")
    lazy val toDateFields = Seq("toDate.day" -> "15", "toDate.month" -> "1", "toDate.year" -> "2003")
    lazy val validFormData = Seq("capacity" -> validRelationship) ++ fromDateFields ++ toDateFields
  }
}




