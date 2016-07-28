package useCaseSpecs

import useCaseSpecs.utils.{AccountID, FrontendTest, Page, PropertyLink, SessionID, UIFormats}

class ManageProperties extends FrontendTest {
  import TestData._

  "Given an interested person is logged in and has previously claimed properties" - {
    implicit val sid: SessionID = sessionId
    implicit val aid: AccountID = accountId
    HTTP.stubLinkedPropertiesAPI(accountId, addedProperties, pendingProperties)

    "When they navigate to manage properties page" - {
      val page = Page.get("/property-linking/manage-properties")

      "They are shown the properties that they have linked to" in {
        addedProperties.foreach { p =>
          page.mustContainDataRow(p.name, p.billingAuthorityReference, p.capacity, "added", UIFormats.date(p.linkedDate))
        }
      }

      "And they are shown the properties who they have attempted to link to but are in the pending state" in {
        pendingProperties.foreach { p =>
          page.mustContainDataRow(p.name, p.billingAuthorityReference, p.capacity, "pending", UIFormats.date(p.linkedDate))
        }
      }

      "And they are able to add another property" in {
        page.mustContainLink("#addAnotherProperty", "/property-linking/property-search")
      }
    }
  }

  object TestData {
    lazy val sessionId = java.util.UUID.randomUUID.toString
    lazy val accountId = "adsjflkj3243"
    lazy val addedProperties = Seq(
      PropertyLink("The House in The City", "ge443asdas", "owner", "23-03-2009"),
      PropertyLink("The Other House in The Other City", "er23asdfas", "owner", "23-03-2012"),
      PropertyLink("The House in The Other City", "aad3ge443asdas", "owner", "23-03-2013"),
      PropertyLink("The Other House in The City", "ge44Xwers", "owner", "23-03-2014")
    )
    lazy val pendingProperties = Seq(
      PropertyLink("The Pending House in The Pending City", "gBADewaa", "owner", "12-12-2015"),
      PropertyLink("The Other Pending House in The Other Pending City", "gBADewaa", "owner", "12-02-2015")
    )
  }
}