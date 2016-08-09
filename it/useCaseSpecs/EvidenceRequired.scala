package useCaseSpecs

import useCaseSpecs.utils._

class EvidenceRequired extends FrontendTest {
  import TestData._

  "Given an interested person is being asked to provide additional evdidence" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    implicit val aid: AccountID = "sdfksjdlf34233gr6"
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

    "When they specify they have evidence and upload upto 3 files" - {
      HTTP.stubFileUpload(aid, sid, "evidence", ("file1.pdf", bytes1), ("file2.pdf", bytes2))
      val result = Page.postValid("/property-linking/upload-evidence", "hasEvidence" -> "doeshaveevidence")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are taken to the evidence uploaded confirmation page" in {
        result.header.headers("location") mustEqual "/property-linking/evidence-uploaded"
      }
    }

    "But if they specify they do not have any evidence" - {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = "ggtttasdfkjasldjflasjd2"
      HTTP.stubKeystoreSession(SessionDocument(property, Some(declaration)))
      val result = Page.postValid("/property-linking/upload-evidence", "hasEvidence" -> "doesnothaveevidence")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are taken to the no evidence provided page" in {
        result.header.headers("location") mustEqual "/property-linking/no-evidence-uploaded"
      }
    }
  }

  object TestData {
    lazy val baRef = "asdfjlj23l4j23"
    lazy val address = Address(Seq.empty, "AA11 1AA")
    lazy val property = Property(baRef, address, false, false)
    lazy val declaration = CapacityDeclaration("occupier", "03-10-2003", None)
    lazy val bytes1 = (44 to 233).map(_.toByte).toArray
    lazy val bytes2 = (200 to 433).map(_.toByte).toArray
  }
}
