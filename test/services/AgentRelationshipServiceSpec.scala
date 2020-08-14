/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{DefaultPaginationParams, PaginationParams}
import models._
import models.propertyrepresentation.AgentAppointmentChangesResponse
import models.searchApi.OwnerAuthAgent
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.Future
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import tests.AllMocks

class AgentRelationshipServiceSpec extends ServiceSpec with AllMocks {

  import scala.concurrent.ExecutionContext.Implicits.global

  when(mockApplicationConfig.agentAppointDelay).thenReturn(0)

  private lazy val testService = new AgentRelationshipService(
    mockAuditingService,
    mockRepresentationConnector,
    mockPropertyLinkConnector,
    mockSessionRepo,
    mockApplicationConfig)

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockSessionRepo = mock[SessionRepo]

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("1111")))

  "getMyAgentPropertyLinks" should "return OwnerAuthResult when successful" in {
    when(mockPropertyLinkConnector.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    val res = testService.getMyAgentPropertyLinks(1, GetPropertyLinksParameters(), DefaultPaginationParams)

    res.futureValue must be(ownerAuthResultWithTwoAuthorisation)

    verify(mockPropertyLinkConnector, times(1)).getMyAgentPropertyLinks(any(), any(), any())(any())
  }

  "getMyOrganisationAgents" should "return AgentList when successful" in {
    when(mockPropertyLinkConnector.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList))
    val res = testService.getMyOrganisationAgents()

    res.futureValue must be(organisationsAgentsList)

    verify(mockPropertyLinkConnector, times(1)).getMyOrganisationAgents()(any())
  }

  "createAndSubmitAgentRepRequest" should "return option unit when successful when new agent relationship is enabled" in {

    val links = SessionPropertyLinks(
      Seq(
        SessionPropertyLink(1L, "1", Seq(OwnerAuthAgent(1l, 1l, "organisationName", 1l)))
      )
    )

    when(mockPropertyLinkConnector.assignAgentToSomeProperties(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-change-id")))

    val res = testService.createAndSubmitAgentRepRequest(
      pLinkIds = List("1"),
      agentOrgId = 1L,
      organisationId = 1L,
      individualId = 1L,
      isAgent = true,
      agentCode = 12312L
    )

    res.futureValue must be(())

    verify(mockPropertyLinkConnector, times(1)).assignAgentToSomeProperties(any())(any())
  }

  "createAndSubitAgentRevokeRequest" should "return option unit when successful when new agent relationship enabled" in {

    val links =
      SessionPropertyLinks(Seq(SessionPropertyLink(1L, "1", Seq(OwnerAuthAgent(1l, 1l, "organisationName", 1l)))))

    when(mockPropertyLinkConnector.unassignAgentFromSomeProperties(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-change-id")))

    val res = testService.createAndSubmitAgentRevokeRequest(List("1"), 1L)

    res.futureValue must be(())

    verify(mockPropertyLinkConnector).unassignAgentFromSomeProperties(any())(any())
  }

  "getAgentNameAndAddress" should "return agent details" in {
    when(mockRepresentationConnector.getAgentDetails(any())(any())).thenReturn(Future.successful(Some(agentDetails)))

    testService.getAgentNameAndAddress(125L).futureValue mustBe Some(agentDetails)
  }

  "getMyOrganisationPropertyLinksCount" should "return organisation property links count" in {
    val propertyLinksCount = 1

    when(mockPropertyLinkConnector.getMyOrganisationPropertyLinksCount())
      .thenReturn(Future.successful(propertyLinksCount))

    testService.getMyOrganisationPropertyLinksCount().futureValue mustBe propertyLinksCount
  }
}
