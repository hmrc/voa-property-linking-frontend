package useCaseSpecs

import config.Wiring
import useCaseSpecs.utils._

class ManageProperties extends FrontendTest {
  import TestData._

  "Given an interested person is logged in and has previously claimed properties" - {
    implicit val sid: SessionID = sessionId
    implicit val aid: AccountID = accountId
    HTTP.stubLinkedPropertiesAPI(accountId, addedProperties, pendingProperties)
    HTTP.stubAccounts(Seq(Account(accountId, false)))
    addedProperties.map ( prop =>
      HTTP.stubPropertyRepresentationAPI(accountId, prop.uarn)
    )
    pendingProperties.map ( prop =>
      HTTP.stubPropertyRepresentationAPI(accountId, prop.uarn)
    )

    properties.map( prop =>
      HTTP.stubPropertiesAPI(prop.uarn, prop)
    )

    "When they navigate to manage properties page" - {
      val page = Page.get("/property-linking/manage-properties")

      "They are shown the properties that they have linked to" in {
        addedProperties.foreach { p =>
          page.mustContainDataInRow(
          "Some where, postcode", p.uarn, p.capacityDeclaration.capacity, "added", /*UIFormats.date(p.linkedDate), */p.assessmentYears.mkString(","), "Add agent", " Edit agent"
          )
        }
      }

      "And they are shown the properties who they have attempted to link to but are in the pending state" in {
        pendingProperties.foreach { p =>
          page.mustContainDataInRow("Some where, postcode", p.uarn, p.capacityDeclaration.capacity, "pending", /*UIFormats.date(p.linkedDate),*/ "")
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
      PropertyLink("uarn1", accountId, CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2009, 2014), false),
      PropertyLink("uarn2", accountId, CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2009), false),
      PropertyLink("uarn3", "aad3ge443asdas", CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2014), false),
      PropertyLink("uarn4", "ge44Xwers", CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2004), false)
    )
    lazy val pendingProperties = Seq(
      PropertyLink("uarn5", accountId, CapacityDeclaration("ownerlandlord", "2010-01-01", None), "2012-01-01", Seq(), true),
      PropertyLink("uarn6", "gBADewaa", CapacityDeclaration("ownerlandlord", "2010-01-01", None), "2012-01-01", Seq(), true)
    )
    lazy val properties = Seq(
      Property("uarn1", "baRes", Address(Seq("Some where", "over the rainbow"), "postcode"), true, true),
      Property("uarn2", "baRes", Address(Seq("Some where"), "postcode"), true, true),
      Property("uarn3", "baRes", Address(Seq("Some where"), "postcode"), true, true),
      Property("uarn4", "baRes", Address(Seq("Some where"), "postcode"), true, true),
      Property("uarn5", "baRes", Address(Seq("Some where"), "postcode"), true, true),
      Property("uarn6", "baRes", Address(Seq("Some where"), "postcode"), true, true)
    )
  }
}