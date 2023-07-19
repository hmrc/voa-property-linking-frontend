package connectors.propertyLinking

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import models.propertyrepresentation.{AgentAppointmentChangeRequest, AgentAppointmentChangesResponse, AgentList, AgentSummary, AppointmentAction, AppointmentScope}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class PropertyLinkConnectorISpec extends ISpecBase {
  val connector: PropertyLinkConnector = app.injector.instanceOf[PropertyLinkConnector]

  "getMyOrganisationAgents" should {
    "return the organisation's agent data" in {
      implicit val testHeaderCarrier: HeaderCarrier = HeaderCarrier()

      val testAppointedDate = LocalDate.now()

      val testAgentList = AgentList(
        resultCount = 1,
        agents = List(
          AgentSummary(
            organisationId = 12345L,
            representativeCode = 12345L,
            name = "testName",
            appointedDate = testAppointedDate,
            propertyCount = 0,
            listYears = Some(Seq("2017"))
          )
        )
      )

      val jsonResponse = Json.obj(
        "resultCount" -> 1,
        "agents" -> Json.arr(
          Json.obj(
            "organisationId"     -> 12345,
            "representativeCode" -> 12345,
            "name"               -> "testName",
            "appointedDate"      -> Json.toJson(testAppointedDate),
            "propertyCount"      -> 0,
            "listYears" -> Json.arr(
              "2017"
            )
          )
        )
      )

      stubFor(
        get("/property-linking/owner/agents")
          .willReturn(
            aResponse
              .withStatus(OK)
              .withBody(jsonResponse.toString())
          )
      )

      await(connector.getMyOrganisationAgents()) shouldBe testAgentList
    }
  }
  
  "agentAppointmentChange (POST)" should {
    "return a successful AgentAppointmentChangesResponse" in {
      implicit val testHeaderCarrier: HeaderCarrier = HeaderCarrier()

      val agentChangeResponse = AgentAppointmentChangesResponse(appointmentChangeId = "1")

      val agentChangeRequest = AgentAppointmentChangeRequest(
        agentRepresentativeCode = 123456,
        scope = AppointmentScope.PROPERTY_LIST,
        action = AppointmentAction.APPOINT,
        propertyLinkIds = Some(List("123L")),
        listYears = Some(List("2023")))

      val jsonRequest = Json.parse("""{
                           |   "agentRepresentativeCode":123456,
                           |   "action":"APPOINT",
                           |   "scope":"PROPERTY_LIST",
                           |   "propertyLinkIds":[
                           |      "123L"
                           |   ],
                           |   "listYears":[
                           |      "2023"
                           |   ]
                           |}""".stripMargin)

      val jsonResponse = Json.obj("appointmentChangeId" -> "1")

      stubFor(
        post("/property-linking/my-organisation/agent/submit-appointment-changes")
          .withRequestBody(equalToJson(jsonRequest.toString(), true, false))
          .willReturn(
            aResponse
              .withStatus(CREATED)
              .withBody(jsonResponse.toString())
          )
      )

      await(connector.agentAppointmentChange(agentChangeRequest)) shouldBe agentChangeResponse
    }
  }
}
