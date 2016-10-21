package useCaseSpecs

import useCaseSpecs.utils._

class SelfCertificationConfirmation extends FrontendTest {

  import TestData._

  "Given an interested person has self certified for a property" - {
    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
    implicit val session = GGSession(userId, token)
    HTTP.stubAuthentication(session)
    HTTP.stubGroupId(session, groupId)
    HTTP.stubKeystoreSession(SessionDocument(selfCertProperty, envelopeId, Some(declaration), selfCertifyComplete = Some(true)), Seq(Account(userId, false)))

    "When they arrive at the self certification confirmation page" - {
      val page = Page.get("/property-linking/self-certification-link-submitted")

      "They see confirmation that their property linking request has been submitted" in {
        page.mustContainSuccessSummary(s"Thank you for your request which has been submitted to the Valuation Office Agency.")
      }
      "And the page contains a link to the dashboard" in {
        page.mustContainLink("#backToDashBoard", "/property-linking/manage-properties")
      }
    }
  }

  "Given an interested person has not self certified for a property" - {
    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
    implicit val session = GGSession(userId, token)
    HTTP.stubAuthentication(session)
    HTTP.stubKeystoreSession(SessionDocument(selfCertProperty, envelopeId, Some(declaration)), Seq(Account(userId, false)))

    "When they try to access the self certification page" - {
      val res = Page.getResult("/property-linking/self-certification-link-submitted")

      "They are redirected to the dashboard home page" in {
        res.header.status mustEqual 303
        res.header.headers("location") mustEqual "/property-linking/home"
      }
    }
  }

  object TestData {
    lazy val baRef = "sfku03802342"
    lazy val uarn = "uarn03802342"
    lazy val envelopeId = "asdfasfasf"
    lazy val address = Address("leen1", "leen2", "leen3", "AA11 1AA")
    lazy val userId = "389u4asldkjfasljdf"
    lazy val token = "uoashf98ouafn"
    lazy val groupId = "98afh98ahuo32inm"
    lazy val formattedAddress = "leen1, leen2, leen3, AA11 1AA"
    lazy val selfCertProperty = Property(uarn, baRef, address, true, true)
    lazy val declaration = CapacityDeclaration("occupier", "2001-01-01", None)
  }

}
