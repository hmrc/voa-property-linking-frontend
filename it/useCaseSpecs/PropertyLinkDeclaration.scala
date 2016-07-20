package useCaseSpecs

import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId
import useCaseSpecs.utils._

class PropertyLinkDeclaration extends FrontendTest {
  import TestData._

  "Given an interested person is logged in and has selected a property that exists to claim" - {
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
    HTTP.stubPropertiesAPI(propertyToClaimBillingAuthorityRef, propertyToClaim)

    "When they arrive at the declaration page" - {
      val page = Page.get(s"/property-linking/link-to-property/$propertyToClaimBillingAuthorityRef")

      "They are asked to provide:" - {

        "their relationship to the property" in {
          page.mustContainRadioSelect("capacity", Seq("occupier", "owner", "previousOccupier"))
        }

        "the date they became an interested person of the selected property" in {
          page.mustContainDateSelect("fromDate")
        }

        "the date they ceased to be an interested person of the selected property" in {
          page.mustContainDateSelect("toDate")
        }
      }
    }

    // TODO - unit tests for the different types of property
    "When they supply a valid relationship, start and end date" ignore {
      val formData = Seq("relationship" -> validRelationship, "fromDate" -> fromDate, "toDate" -> toDate, "baRef" -> propertyToClaimBillingAuthorityRef)
      val response = Page.postValid("", formData:_*)

      "Their declaration is stored in the session" in {
        fail()
      }

      "And they continue to the next step of their journey" in {
        response.header.status mustEqual 303
        response.header.headers("location") mustEqual "TODO"
      }
    }

    // TODO - unit tests for the different types of inputs and validation rules
    "When they do not supply a valid relationship, start or end date" ignore {
      val formData = Seq("relationship" -> invalidRelationship, "fromDate" -> fromDate, "toDate" -> toDate, "baRef" -> propertyToClaimBillingAuthorityRef)
      val page = Page.postInvalid("", formData:_*)

      "An error summary is shown" in {
        page.mustContainSummaryErrors("capacity" -> "Must supply capacity")
      }

      "A field-level error is shown for each invalid field" in {
        page.mustContainFieldErrors("capacity" -> "Must supply relationship")
      }
    }
  }

  object TestData {
    lazy val sessionId = java.util.UUID.randomUUID().toString
    lazy val propertyToClaimBillingAuthorityRef = "blahblahblooh"

    lazy val address = Address(Seq.empty, "AA11 1AA", true)
    lazy val propertyToClaim = Property(propertyToClaimBillingAuthorityRef, address, "shop", false, true)
    lazy val validRelationship = "owner"
    lazy val invalidRelationship = "re44wo"
    lazy val fromDate = "23/12/2001"
    lazy val toDate = "15/01/2003"
  }
}




