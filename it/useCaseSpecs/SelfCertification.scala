package useCaseSpecs

import org.joda.time.DateTime
import useCaseSpecs.utils.{Property, _}

class SelfCertification extends FrontendTest {

  import TestData._

  "Given an interested person has declared their capacity to a self-certifiable property" - {
    HTTP.reset()
    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
    implicit val session = GGSession(userId, token)
    HTTP.stubKeystoreSession(SessionDocument(selfCertifiableProperty, Some(declaration)), Seq(Account(userId, false)))
    HTTP.stubAuthentication(session)
    HTTP.stubGroupId(session, groupId)

    "When they arrive at the the self certification page" - {
      val page = Page.get("/property-linking/self-certify")

      "They can see the self certification terms and conditions" in {
        page.mustContain1("#self-certification-statement")
      }

      "And they are asked to agree to the terms and conditions" in {
        page.mustContainCheckbox("iAgree")
      }
    }

    "When they agree to the self certification terms and conditions" - {
      val response = Page.postValid("/property-linking/confirm-self-certify", "iAgree" -> "true")

      "Their link request is submitted" in {
        HTTP.verifyPropertyLinkRequest(uarn, groupId, expectedLink)
      }

      "And they are redirected the self declaration property linking submission" in {
        response.header.headers("location") mustEqual "/property-linking/self-certification-link-submitted"
      }

      "And a flag is marked in the session indicating the link was successful" in {
        HTTP.verifyKeystoreSaved(SessionDocument(selfCertifiableProperty, Some(declaration), Some(true)))
      }
    }

    "But if a user does not agree to the self certification terms and conditions" - {
      val page = Page.postInvalid("/property-linking/confirm-self-certify", "iAgree" -> "false")

      "Their link request is not submitted" in {
        HTTP.verifyNoMoreLinkRequests(1)
      }

      "There is an error summary indicating they are required to accept" in {
        page.mustContainSummaryErrors(("iAgree", "Do you agree?", "You must agree to continue."))
      }

      "There is a field-level error on the agreement confirmation" in {
        page.mustContainFieldErrors("iAgree" -> "You must agree to continue.")
      }
    }
  }

  object TestData {
    lazy val uarn = "uarn6"
    lazy val userId = "bizn33z123xdr"
    lazy val token = "jaknsfpagklm"
    lazy val groupId = "oisagnodkflka"
    lazy val address = Address("", "", "", "AA11 1AA")
    lazy val selfCertifiableProperty = Property(uarn, "xyzbaref332", address, true, true)
    lazy val declaration = CapacityDeclaration("occupier", "2011-01-01", None)
    lazy val expectedLink = PropertyLink(uarn, groupId, declaration, DateTime.now.toString("YYYY-MM-dd"), Seq(2017), true, "selfCertify")
  }

}
