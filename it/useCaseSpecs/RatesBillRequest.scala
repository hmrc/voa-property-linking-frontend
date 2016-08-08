package useCaseSpecs

import useCaseSpecs.utils._

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
      HTTP.stubFileUpload(aid, sid, "ratesBill", ("ratesbill.pdf", validRatesBill))
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are sent to the rates bill accepted page" in {
        result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-approved"
      }
    }

    "But if they supply a rates bill that cannot be immediately verified" - {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = "3874sjflkaj234"
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)))
      HTTP.stubRatesBillCheck(baRef, invalidRatesBill, ratesBillAccepted = false)
      HTTP.stubFileUpload(aid, sid, "ratesBill", ("ratesbill.pdf", invalidRatesBill))
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are sent to the rates bill pending page" in {
        result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-approval-pending"
      }
    }

    "However, if they specify they do not have a rates bill & the property can receive mail" - {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = "93ddreqejkasdfasd"
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)))
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doesnothaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are notified that they are on the postal PIN process journey" in {
        result.header.headers.get("location").value mustEqual "/property-linking/pin-postal-process"
      }
    }

    "However, if they specify they do not have a rates bill & the property cannot receive mail" - {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = "93ddreqejkasdfasd"
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertNoMailProperty, Some(declaration)))
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doesnothaveratesbill")

      "No request is submitted" in {
        HTTP.verifyNoPropertyLinkRequest(baRef, aid)
      }

      "They are sent on the additional evidence journey" in {
        result.header.headers.get("location").value mustEqual "/property-linking/upload-evidence"
      }
    }

    "When they do not supply valid response" - {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = "bbkljier2333as"
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)))
      HTTP.stubFileUploadWithNoFile(aid, sid, "ratesBill")
      val page = Page.postInvalid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "An error summary is shown" in {
        page.mustContainSummaryErrors(("ratesBill", "Please upload a copy of the rates bill", "please select a rates bill"))
      }

      "A field-level error is shown for each invalid field" in {
        page.mustContainFieldErrors(("ratesBill", "please select a rates bill"))
      }
    }
  }

  object TestData {
    lazy val baRef = "34asdf23423"
    lazy val address = Address(Seq("Hooooohhhhhhaaaaaqaaaeeee"), "AA11 1AA")
    lazy val nonSelfCertProperty = Property(baRef, address, isSelfCertifiable = false, true)
    lazy val nonSelfCertNoMailProperty = Property(baRef, address, isSelfCertifiable = false, canReceiveMail = false)
    lazy val declaration = CapacityDeclaration("occupier", "01-01-2012", None)
    lazy val validRatesBill = (1 to 1000).map(_.toByte).toArray
    lazy val invalidRatesBill = (5000 to 7000).map(_.toByte).toArray
  }
}
