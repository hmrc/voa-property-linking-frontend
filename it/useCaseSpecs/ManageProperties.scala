package useCaseSpecs

import config.Wiring
import controllers.Account
import useCaseSpecs.utils._

class ManageProperties extends FrontendTest {
  import TestData._

  "Given an interested person is logged in and has previously claimed properties" - {
    implicit val sid: SessionID = sessionId
    implicit val aid: AccountID = accountId
    HTTP.stubLinkedPropertiesAPI(accountId, addedProperties, pendingProperties)
    Wiring().tmpInMemoryAccountDb(accountId) = Account(accountId, false)
    addedProperties.map ( prop =>
      HTTP.stubPropertyRepresentationAPI(accountId, prop.uarn)
    )
    pendingProperties.map ( prop =>
      HTTP.stubPropertyRepresentationAPI(accountId, prop.uarn)
    )

    "When they navigate to manage properties page" - {
      val page = Page.get("/property-linking/manage-properties")

      "They are shown the properties that they have linked to" in {
        addedProperties.foreach { p =>
          page.mustContainDataInRow(
            p.name, p.billingAuthorityReference, p.capacity, "added", UIFormats.date(p.linkedDate), p.assessmentYears.mkString(","), "Add agent", "Edit agent"
          )
        }
      }

      "And they are shown the properties who they have attempted to link to but are in the pending state" in {
        pendingProperties.foreach { p =>
          page.mustContainDataInRow(p.name, p.billingAuthorityReference, p.capacity, "pending", UIFormats.date(p.linkedDate), "")
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
      PropertyLink("The House in The City", "uarn1", "ge443asdas", "owner", "23-03-2009", Seq(2009, 2014)),
      PropertyLink("The Other House in The Other City", "uarn2", "er23asdfas", "owner", "23-03-2012", Seq(2009)),
      PropertyLink("The House in The Other City", "uarn3", "aad3ge443asdas", "owner", "23-03-2013", Seq(2014)),
      PropertyLink("The Other House in The City", "uarn4", "ge44Xwers", "owner", "23-03-2014", Seq(2004))
    )
    lazy val pendingProperties = Seq(
      PendingPropertyLink("The Pending House in The Pending City", "uarn5", "gBADewaa", "owner", "12-12-2015"),
      PendingPropertyLink("The Other Pending House in The Other Pending City", "uarn6", "gBADewaa", "owner", "12-02-2015")
    )
  }
}