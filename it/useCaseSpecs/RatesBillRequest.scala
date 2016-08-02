package useCaseSpecs

import useCaseSpecs.utils.{AccountID, Address, CapacityDeclaration, FrontendTest, Page, Property, SessionDocument, SessionID}

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
  }

  object TestData {
    lazy val baRef = "34asdf23423"
    lazy val address = Address(Seq("Hooooohhhhhhaaaaaqaaaeeee"), "AA11 1AA")
    lazy val nonSelfCertProperty = Property(baRef, address, isSelfCertifiable = false, true)
    lazy val declaration = CapacityDeclaration("occupier", "01-01-2012", None)
  }
}
