package useCaseSpecs

import play.api.test.Helpers._
import useCaseSpecs.utils.{AccountID, Address, CapacityDeclaration, FrontendTest, Page, Property, SessionDocument, SessionID}

import scala.concurrent.Future

class RatesBillRequest extends FrontendTest {
  import TestData._

  "Given an interested person was unable to self certify for a property" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    implicit val aid: AccountID = "sdfksjdlf342"
    HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)))

    "When they arrive at the rates bill request page" - {
      val page = Page.get("/property-linking/supply-rates-bill")

      "They are asked if they have a rates bill to upload for the selected property" in {
        page.mustContainRadioSelect("hasRatesBill", Seq("doeshaveratesbill", "doesnothaveratesbill"))
      }
    }

    "When they specify they have a rates bill, and upload a rates bill that can be instantly verified" - {
      HTTP.stubRatesBillCheck(baRef, validRatesBill, ratesBillAccepted = true)
      HTTP.stubFileUpload(aid, sid, "ratesBill", "ratesbill.pdf", validRatesBill)
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "They are sent to the rates bill accepted page" in {
        result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-approved" 
      }
    }

    "But if they supply a rates bill that cannot be immediately verified" ignore {

      "They are sent to the rates bill not accepted page" in {
        fail()
      }
    }

    "However, if they specify they do not have a rates bill" ignore {

      "They are sent on the postal PIN process journey" in {
        fail()
      }
    }

    "When they do not supply valid response" ignore {

      "An error summary is shown" in {
        fail()
      }

      "A field-level error is shown for each invalid field" in {
        fail()
      }
    }
  }

  object TestData {
    lazy val baRef = "34asdf23423"
    lazy val address = Address(Seq("Hooooohhhhhhaaaaaqaaaeeee"), "AA11 1AA")
    lazy val nonSelfCertProperty = Property(baRef, address, isSelfCertifiable = false, true)
    lazy val declaration = CapacityDeclaration("occupier", "01-01-2012", None)
    lazy val validRatesBill = (1 to 1000).map(_.toByte).toArray
  }
}
