package useCaseSpecs

import useCaseSpecs.utils.{Property, _}

class SelfCertification extends FrontendTest {
  import TestData._

  "Given an interested person has declared their capacity to a self-certifiable property" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    HTTP.stubKeystoreSession(SessionDocument(selfCertifiableProperty, Some(capacityDeclaration)))

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
      val response = Page.postValidWithAccount("/property-linking/confirm-self-certify", "iAgree" -> "true")(accountId)

      "Their link request is submitted" in {
        val submission = LinkToProperty(CapacityDeclaration(capacityDeclaration.capacity, capacityDeclaration.fromDate, None))
        HTTP.verifyPropertyLinkRequest(selfCertifiableProperty.billingAuthorityReference, accountId, submission)
      }

      "And they are redirected the self declaration confirmation screen once the link is successfully confirmed" in {
        response.header.headers("location") mustEqual "/property-linking/self-certification-link-authorised"
      }
    }

    "But if a user does not agree to the self certification terms and conditions" ignore {

      "Their link request is not submitted" in {
        fail()
      }

      "Their is an error summary indicating they are required to accept" in {
        fail()
      }

      "There is a field-level error on the agreement confirmation" in {
        fail()
      }
    }
  }

  object TestData {
    lazy val accountId = "bizn33z123xdr"
    lazy val address = Address(Seq.empty, "AA11 1AA", true)
    lazy val selfCertifiableProperty = Property("xyzbaref332", address, "shop", false, true)
    lazy val capacityDeclaration = CapacityDeclaration("occupier", "01-01-2011", None)
  }
}
