package useCaseSpecs

import org.joda.time.DateTime
import useCaseSpecs.utils._

class EvidenceRequired extends FrontendTest {

  import TestData._

  "Given an interested person is being asked to provide additional evidence" - {
    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
    implicit val session = GGSession(userId, token)
    HTTP.stubKeystoreSession(SessionDocument(property, Some(declaration)), Seq(Account(userId, false)))
    HTTP.stubAuthentication(session)

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
      HTTP.stubFileUpload(userId, sid, "evidence", ("file1.pdf", bytes1), ("file2.pdf", bytes2))
      val result = Page.postValid("/property-linking/upload-evidence", "hasEvidence" -> "doeshaveevidence")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(uarn, userId, expectedLink)
      }

      "They are taken to the evidence uploaded confirmation page" in {
        result.header.headers("location") mustEqual "/property-linking/evidence-uploaded"
      }
    }

    "But if they specify they do not have any evidence" - {
      implicit val sid: SessionId = java.util.UUID.randomUUID.toString
      HTTP.stubKeystoreSession(SessionDocument(property, Some(declaration)), Seq(Account(userId, false)))
      val result = Page.postValid("/property-linking/upload-evidence", "hasEvidence" -> "doesnothaveevidence")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(uarn, userId, expectedLink)
      }

      "They are taken to the no evidence provided page" in {
        result.header.headers("location") mustEqual "/property-linking/no-evidence-uploaded"
      }
    }

    "When they do not supply a valid response" - {
      implicit val sid: SessionId = java.util.UUID.randomUUID.toString
      HTTP.stubKeystoreSession(SessionDocument(property, Some(declaration)), Seq(Account(userId, false)))
      HTTP.stubFileUpload(userId, sid, "evidence", ("1.pdf", bytes1), ("2.pdf", bytes1), ("3.pdf", bytes1), ("4.pdf", bytes1))
      val page = Page.postInvalid("/property-linking/upload-evidence", "hasEvidence" -> "doeshaveevidence")

      "An error summary is shown" in {
        page.mustContainSummaryErrors(
          ("evidence", "Please upload evidence so that we can verify your link to the property.", "Only 3 files may be uploaded")
        )
      }

      "A field-level error is shown for each invalid field" in {
        page.mustContainFieldErrors("evidence" -> "Only 3 files may be uploaded")
      }
    }
  }

  object TestData {
    lazy val uarn = "uarn4"
    lazy val baRef = "baRef-asdfjlj23l4j23"
    lazy val userId = "sdfksjdlf34233gr6"
    lazy val token = "jaslasknal;;"
    lazy val address = Address(Seq.empty, "AA11 1AA")
    lazy val property = Property(uarn, baRef, address, false, false)
    lazy val declaration = CapacityDeclaration("occupier", "2003-10-03", None)
    lazy val bytes1 = (44 to 233).map(_.toByte).toArray
    lazy val bytes2 = (200 to 433).map(_.toByte).toArray
    lazy val expectedLink = PropertyLink(uarn, userId, declaration, DateTime.now.toString("YYYY-MM-dd"), Seq(2017), true, "otherEvidence")

  }

}
