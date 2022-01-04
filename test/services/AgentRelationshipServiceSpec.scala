/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import binders.propertylinks.GetPropertyLinksParameters
import connectors.PropertyRepresentationConnector
import controllers.DefaultPaginationParams
import models.propertyrepresentation.AgentAppointmentChangesResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import repositories.SessionRepo
import tests.AllMocks
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future

class AgentRelationshipServiceSpec extends ServiceSpec with AllMocks {

  when(mockApplicationConfig.agentAppointDelay).thenReturn(0)

  private lazy val testService =
    new AgentRelationshipService(mockRepresentationConnector, mockPropertyLinkConnector, mockSessionRepo)

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockSessionRepo = mock[SessionRepo]

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("1111")))

  "getMyAgentPropertyLinks" should {
    "return OwnerAuthResult when successful" in {
      when(mockPropertyLinkConnector.getMyAgentPropertyLinks(any(), any(), any())(any()))
        .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
      val res = testService.getMyAgentPropertyLinks(1, GetPropertyLinksParameters(), DefaultPaginationParams)

      res.futureValue should be(ownerAuthResultWithTwoAuthorisation)

      verify(mockPropertyLinkConnector, times(1)).getMyAgentPropertyLinks(any(), any(), any())(any())
    }
  }

  "getMyOrganisationAgents" should {
    "return AgentList when successful" in {
      when(mockPropertyLinkConnector.getMyOrganisationAgents()(any()))
        .thenReturn(Future.successful(organisationsAgentsList))
      val res = testService.getMyOrganisationAgents()

      res.futureValue should be(organisationsAgentsList)

      verify(mockPropertyLinkConnector, times(1)).getMyOrganisationAgents()(any())
    }
  }

  "createAndSubmitAgentRepRequest" should {
    "return option unit when successful when new agent relationship is enabled" in {

      when(mockPropertyLinkConnector.assignAgentToSomeProperties(any())(any()))
        .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-change-id")))

      val res = testService.createAndSubmitAgentRepRequest(pLinkIds = List("1"), agentCode = 12312L)

      res.futureValue should be(())

      verify(mockPropertyLinkConnector, times(1)).assignAgentToSomeProperties(any())(any())
    }
  }

  "createAndSubitAgentRevokeRequest" should {
    "return option unit when successful when new agent relationship enabled" in {

      when(mockPropertyLinkConnector.unassignAgentFromSomeProperties(any())(any()))
        .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-change-id")))

      val res = testService.createAndSubmitAgentRevokeRequest(List("1"), 1L)

      res.futureValue should be(())

      verify(mockPropertyLinkConnector).unassignAgentFromSomeProperties(any())(any())
    }
  }

  "getAgentNameAndAddress" should {
    "return agent details" in {
      when(mockRepresentationConnector.getAgentDetails(any())(any())).thenReturn(Future.successful(Some(agentDetails)))

      testService.getAgentNameAndAddress(125L).futureValue shouldBe Some(agentDetails)
    }
  }

  "getMyOrganisationPropertyLinksCount" should {
    "return organisation property links count" in {
      val propertyLinksCount = 1

      when(mockPropertyLinkConnector.getMyOrganisationPropertyLinksCount())
        .thenReturn(Future.successful(propertyLinksCount))

      testService.getMyOrganisationPropertyLinksCount().futureValue shouldBe propertyLinksCount
    }
  }
}
