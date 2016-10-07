package useCaseSpecs

import useCaseSpecs.utils._

class ManageProperties extends FrontendTest {

  import TestData._

  "Given an interested person is logged in and has previously claimed properties" - {
    implicit val sid: SessionId = sessionId
    implicit val session = GGSession(userId, token)
    HTTP.stubAuthentication(session)
    HTTP.stubLinkedPropertiesAPI(userId, addedProperties, pendingProperties)
    HTTP.stubAccounts(Seq(Account(userId, false)))
    addedProperties.foreach(prop =>
      HTTP.stubPropertyRepresentationAPI(userId, prop.uarn)
    )
    pendingProperties.foreach(prop =>
      HTTP.stubPropertyRepresentationAPI(userId, prop.uarn)
    )

    properties.foreach(prop =>
      HTTP.stubPropertiesAPI(prop.uarn, prop)
    )

    "When they navigate to manage properties page" - {
      val page = Page.get("/property-linking/manage-properties")

      "They are shown the properties that they have linked to" in {
        addedProperties.foreach { p =>
          page.mustContainDataInRow(
            "Some where, postcode", p.uarn, p.capacityDeclaration.capacity, "added", p.assessmentYears.mkString(","), "Add agent", " Edit agent"
          )
        }
      }

      "And they are shown the properties who they have attempted to link to but are in the pending state" in {
        pendingProperties.foreach { p =>
          page.mustContainDataInRow("Some where, postcode", p.uarn, p.capacityDeclaration.capacity, "pending", "")
        }
      }

      "And they are able to add another property" in {
        page.mustContainLink("#addAnotherProperty", "/property-linking/property-search")
      }
    }
  }

  object TestData {
    lazy val sessionId = java.util.UUID.randomUUID.toString
    lazy val userId = "adsjflkj3243"
    lazy val token = "aifjnadogkjnsaldn"
    lazy val addedProperties = Seq(
      PropertyLink("uarn1", userId, CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2009, 2014), false, "ratesBill"),
      PropertyLink("uarn2", userId, CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2009), false, "ratesBill"),
      PropertyLink("uarn3", "aad3ge443asdas", CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2014), false, "ratesBill"),
      PropertyLink("uarn4", "ge44Xwers", CapacityDeclaration("ownerlandlord", "2010-01-01", Some("2011-01-01")), "2012-01-01", Seq(2004), false, "ratesBill")
    )
    lazy val pendingProperties = Seq(
      PropertyLink("uarn5", userId, CapacityDeclaration("ownerlandlord", "2010-01-01", None), "2012-01-01", Seq(), true, "ratesBill"),
      PropertyLink("uarn6", "gBADewaa", CapacityDeclaration("ownerlandlord", "2010-01-01", None), "2012-01-01", Seq(), true, "ratesBill")
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
