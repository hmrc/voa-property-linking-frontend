package useCaseSpecs

import org.joda.time.DateTime
import useCaseSpecs.utils._

class RatesBillRequest extends FrontendTest {

  import TestData._

  "Given an interested person was unable to self certify for a property" - {
    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
    implicit val session = GGSession(userId, token)
    HTTP.stubAuthentication(session)
    HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)), Seq(Account(userId, false)))

    "When they arrive at the rates bill request page" - {
      HTTP.stubAccounts(Seq(Account(userId, false)))
      val page = Page.get("/property-linking/supply-rates-bill")

      "They are asked if they have a rates bill to upload for the selected property" in {
        page.mustContainRadioSelect("hasRatesBill", Seq("doeshaveratesbill", "doesnothaveratesbill"))
      }
    }

    "When they specify they have a rates bill" - {
      HTTP.stubRatesBillCheck(baRef, validRatesBill, ratesBillAccepted = true)
      HTTP.stubFileUpload(userId, sid, "ratesBill", ("ratesbill.pdf", validRatesBill))
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(uarn, userId, expectedLink)
      }

      "They are sent to the rates bill accepted page" in {
        result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-submitted"
      }
    }

    "However, if they specify they do not have a rates bill" - {
      implicit val sid: SessionId = java.util.UUID.randomUUID.toString
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertNoMailProperty, Some(declaration)), Seq(Account(userId, false)))
      val result = Page.postValid("/property-linking/supply-rates-bill", "hasRatesBill" -> "doesnothaveratesbill")

      "No request is submitted" in {
        HTTP.verifyNoPropertyLinkRequest(baRef, userId)
      }

      "They are sent on the additional evidence journey" in {
        result.header.headers.get("location").value mustEqual "/property-linking/upload-evidence"
      }
    }

    "When they do not supply valid response" - {
      implicit val sid: SessionId = java.util.UUID.randomUUID.toString
      HTTP.stubKeystoreSession(SessionDocument(nonSelfCertProperty, Some(declaration)), Seq(Account(userId, false)))
      HTTP.stubFileUploadWithNoFile(userId, sid, "ratesBill")
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
    lazy val uarn = "uarn11"
    lazy val userId = "sdfksjdlf342"
    lazy val token = "oaishgosafk0awksl"
    lazy val address = Address(Seq("Hooooohhhhhhaaaaaqaaaeeee"), "AA11 1AA")
    lazy val nonSelfCertProperty = Property(uarn, baRef, address, isSelfCertifiable = false, true)
    lazy val nonSelfCertNoMailProperty = Property(uarn, baRef, address, isSelfCertifiable = false, canReceiveMail = false)
    lazy val declaration = CapacityDeclaration("occupier", "2012-01-01", None)
    lazy val validRatesBill = (1 to 1000).map(_.toByte).toArray
    lazy val invalidRatesBill = (5000 to 7000).map(_.toByte).toArray
    lazy val expectedLink = PropertyLink(uarn, userId, declaration, DateTime.now.toString("YYYY-MM-dd"), Seq(2017), true, "ratesBill")
  }

}
