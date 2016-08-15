package useCaseSpecs

import useCaseSpecs.utils._

class SelfCertificationConfirmation extends FrontendTest {
  import TestData._

  "Given an interested person has self certified for a property" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    implicit val aid: AccountID = "389u4asldkjfasljdf"
    HTTP.stubKeystoreSession(SessionDocument(selfCertProperty, Some(declaration), selfCertifyComplete = Some(true)))

    "When they arrive at the self certification confirmation page" - {
      val page = Page.get("/property-linking/self-certification-link-authorised")

      "They see confirmation that they are now linked to the selected property" in {
        page.mustContainSuccessSummary(s"You have successfully added $formattedAddress to your account.")
      }
    }
  }

  "Given an interested person has not self certified for a property" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    implicit val aid: AccountID = "sdfkj348203842"
    HTTP.stubKeystoreSession(SessionDocument(selfCertProperty, Some(declaration)))

    "When they try to access the self certification page" - {
      val res = Page.getResult("/property-linking/self-certification-link-authorised")

      "They are redirected to the dashboard home page" in {
        res.header.status mustEqual 303
        res.header.headers("location") mustEqual "/property-linking/home"
      }
    }
  }

  object TestData {
    lazy val baRef = "sfku03802342"
    lazy val uarn = "uarn03802342"
    lazy val address = Address(Seq("leen1", "leen2", "leen3"), "AA11 1AA")
    lazy val formattedAddress = "leen1, leen2, leen3, AA11 1AA"
    lazy val selfCertProperty = Property(uarn, baRef, address, true, true)
    lazy val declaration = CapacityDeclaration("occupier", "01-01-2001", None)
  }
}
