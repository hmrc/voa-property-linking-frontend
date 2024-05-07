/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        propertyLinks = Some(List("123L")),
        listYears = Some(List("2023"))
      )

      val jsonRequest = Json.parse("""{
                                     |   "agentRepresentativeCode":123456,
                                     |   "action":"APPOINT",
                                     |   "scope":"PROPERTY_LIST",
                                     |   "propertyLinks":[
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
