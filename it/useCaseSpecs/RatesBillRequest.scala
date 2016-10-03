package useCaseSpecs

import config.Wiring
import useCaseSpecs.utils._

class RatesBillRequest extends FrontendTest {

  import TestData._

  "Given an interested person was unable to self certify for a property" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    implicit val aid: AccountID = accountId
    HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)), Seq(Account(accountId, false)))

    "When they arrive at the rates bill request page" - {
      HTTP.stubAccounts(Seq(Account(accountId, false)))
      val page = Page.get("/property-linking/supply-rates-bill")

      "They are asked if they have a rates bill to upload for the selected property" in {
        page.mustContainRadioSelect("hasRatesBill", Seq("doeshaveratesbill", "doesnothaveratesbill"))
      }
    }

    "When they specify they have a rates bill, and upload a rates bill that can be instantly verified" ignore {
      HTTP.stubRatesBillCheck(baRef, validRatesBill, ratesBillAccepted = true)
      HTTP.stubFileUpload(aid, sid, "ratesBill", ("ratesbill.pdf", validRatesBill))
      val result = Page.postValid("/property-linking/property-links/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are sent to the rates bill accepted page" in {
        result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-approved"
      }
    }

    "But if they supply a rates bill that cannot be immediately verified" ignore {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = accountId
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)), Seq(Account(accountId, false)))
      HTTP.stubRatesBillCheck(baRef, invalidRatesBill, ratesBillAccepted = false)
      HTTP.stubFileUpload(aid, sid, "ratesBill", ("ratesbill.pdf", invalidRatesBill))
      val result = Page.postValid("/property-linking/property-links/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are sent to the rates bill pending page" in {
        result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-approval-pending"
      }
    }

    "However, if they specify they do not have a rates bill & the property can receive mail" ignore {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = accountId
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)), Seq(Account(accountId, false)))
      val result = Page.postValid("/property-linking/property-links/supply-rates-bill", "hasRatesBill" -> "doesnothaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(baRef, aid, LinkToProperty(declaration))
      }

      "They are notified that they are on the postal PIN process journey" in {
        result.header.headers.get("location").value mustEqual "/property-linking/pin-postal-process"
      }
    }

    "However, if they specify they do not have a rates bill & the property cannot receive mail" ignore {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = accountId
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertNoMailProperty, Some(declaration)), Seq(Account(accountId, false)))
      val result = Page.postValid("/property-linking/property-links/supply-rates-bill", "hasRatesBill" -> "doesnothaveratesbill")

      "No request is submitted" in {
        HTTP.verifyNoPropertyLinkRequest(baRef, aid)
      }

      "They are sent on the additional evidence journey" in {
        result.header.headers.get("location").value mustEqual "/property-linking/upload-evidence"
      }
    }

    "When they do not supply valid response" ignore {
      implicit val sid: SessionID = java.util.UUID.randomUUID.toString
      implicit val aid: AccountID = accountId
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)), Seq(Account(accountId, false)))
      HTTP.stubFileUploadWithNoFile(aid, sid, "ratesBill")
      val page = Page.postInvalid("/property-linking/property-links/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

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
    lazy val uarn = "uarn11"
    lazy val accountId = "sdfksjdlf342"
    lazy val address = Address(Seq("Hooooohhhhhhaaaaaqaaaeeee"), "AA11 1AA")
    lazy val nonSelfCertProperty = Property(uarn, baRef, address, isSelfCertifiable = false, true)
    lazy val nonSelfCertNoMailProperty = Property(uarn, baRef, address, isSelfCertifiable = false, canReceiveMail = false)
    lazy val declaration = CapacityDeclaration("occupier", "2012-01-01", None)
    lazy val validRatesBill = (1 to 1000).map(_.toByte).toArray
    lazy val invalidRatesBill = (5000 to 7000).map(_.toByte).toArray
  }

}
