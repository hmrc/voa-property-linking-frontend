package useCaseSpecs

import useCaseSpecs.utils.{AccountID, Address, CapacityDeclaration, FrontendTest, Page, Property, SessionDocument, SessionID}

class EvidenceRequired extends FrontendTest {
  import TestData._

  "Given an interested person is being asked to provide additional evdidence" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    implicit val aid: AccountID = "sdfksjdlf342"
    HTTP.stubKeystoreSession(SessionDocument(property, Some(declaration)))

    "When they arrive at the upload evidence page" - {
      val page = Page.get("/property-linking/upload-evidence")

      "They are asked if they have further evidence" in {
        page.mustContainRadioSelect("hasEvidence", Seq("doeshaveevidence", "doesnothaveevidence"))
      }

      "They are able to upload files as evidence" in {
        page.mustContainMultiFileInput("evidence")
      }
    }
  }

  object TestData {
    lazy val address = Address(Seq.empty, "AA11 1AA")
    lazy val property = Property("asdfjlj23l4j23", address, false, false)
    lazy val declaration = CapacityDeclaration("occupier", "03-10-2003", None)
  }
}
