package connectors.propertyLinking

import base.ISpecBase
import binders.propertylinks.GetPropertyLinksParameters
import com.github.tomakehurst.wiremock.client.WireMock._
import controllers.PaginationParams
import models.propertyrepresentation.{AgentList, AgentSummary}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class PropertyLinkConnectorISpec extends ISpecBase {
  val connector: PropertyLinkConnector = app.injector.instanceOf[PropertyLinkConnector]

  "getMyOrganisationAgents" should {
    "return the organisation's agent data" in {
      implicit val testHeaderCarrier: HeaderCarrier = HeaderCarrier()

      val testAgentList = AgentList(
        resultCount = 1,
        agents = List(
          AgentSummary(
            organisationId = 12345L,
            representativeCode = 12345L,
            name = "testName",
            appointedDate = LocalDate.now(),
            propertyCount = 0,
            listYears = Some(Seq("2017"))
          )
        )
      )

      stubFor(
        get("/property-linking/owner/agents")
          .willReturn(
            aResponse
              .withStatus(OK)
              .withBody(Json.toJson(testAgentList).toString())
          )
      )

      await(connector.getMyOrganisationAgents()) shouldBe testAgentList
    }
  }
}
