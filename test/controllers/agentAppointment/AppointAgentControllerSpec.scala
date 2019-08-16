/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.agentAppointment

import binders.pagination.PaginationParameters
import binders.propertylinks.GetPropertyLinksParameters
import connectors.propertyLinking.PropertyLinkConnector
import connectors.{AgentsConnector, Authenticated, PropertyRepresentationConnector}
import controllers.VoaPropertyLinkingSpec
import models._
import models.searchApi._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{HtmlPage, StubAuthentication, StubGroupAccountConnector}
import org.scalacheck.Arbitrary._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppointAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  "appointMultipleProperties" should "show the appoint agent page with a known agent listed for selection with no agent appointment session" in {
    val testOwnerAgents = OwnerAgents(Seq(OwnerAgent("test-agent", 1l)))
    stubLogin()
    when(mockAgentsConnector.ownerAgents(any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAgents))
    val res = testController.appointMultipleProperties()(FakeRequest())
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#middle-radio-buttons")
    page.mustContainText("test-agent")
  }

  "appointMultipleProperties" should "show the appoint agent page with no known agents listed for selection" in {
    val testOwnerAgents = OwnerAgents(Seq())
    stubLogin()
    when(mockAgentsConnector.ownerAgents(any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAgents))
    val res = testController.appointMultipleProperties()(FakeRequest())
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustNotContainTable("#middle-radio-buttons")
  }

  "getAgentsForRemove" should "show the revoke agent page with a known agent listed for selection with no agent appointment session" in {
    val testOwnerAgents = OwnerAgents(Seq(OwnerAgent("test-agent", 1l)))
    stubLogin()
    when(mockAgentsConnector.ownerAgents(any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAgents))
    val res = testController.revokeMultipleProperties()(FakeRequest())
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#middle-radio-buttons")
    page.mustContainText("test-agent")
  }

  "getAgentsForRemove" should "show the appoint agent page with no known agents listed for selection" in {
    val testOwnerAgents = OwnerAgents(Seq())
    stubLogin()
    when(mockAgentsConnector.ownerAgents(any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAgents))
    val res = testController.revokeMultipleProperties()(FakeRequest())
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustNotContainTable("#middle-radio-buttons")
  }


  "selectAgentPropertiesSearchSort" should "show a list of properties available for removal from a agent" in {
    stubLogin()
    val testAgentAccount = arbitrary[GroupAccount].sample.get.copy(isAgent = true, agentCode = 1l)
    val testAgents = Some(Seq(arbitrary[OwnerAuthAgent].sample.get.copy(organisationId = testAgentAccount.id)))
    val testOwnerAuth = arbitrary[OwnerAuthorisation].sample.get.copy(agents = testAgents, status = "APPROVED")
    val testOwnerAuthResult = OwnerAuthResult(start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(testOwnerAuth))
    val testPagination = AgentPropertiesParameters(1l)

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockPropertyLinkConnector.getMyOrganisationsPropertyLinks(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAuthResult))

    val res = testController.selectAgentPropertiesSearchSort(PaginationParameters(), GetPropertyLinksParameters(), 1L)(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "agentCodeRadio" -> "1"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(testOwnerAuth.address)
  }

  "viewing select agent properties search sort page" should "show no properties when the agent has no properties appointed" in {
    stubLogin()
    val testAgentAccount = arbitrary[GroupAccount].sample.get.copy(isAgent = true, agentCode = 1l)
    val testAgents = Some(Seq(arbitrary[OwnerAuthAgent].sample.get.copy(organisationId = testAgentAccount.id)))
    val testOwnerAuth = arbitrary[OwnerAuthorisation].sample.get.copy(agents = testAgents, status = "REVOKED")
    val testOwnerAuthResult = OwnerAuthResult(start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(testOwnerAuth))
    val testPagination = AgentPropertiesParameters(1l)

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockPropertyLinkConnector.getMyClientsPropertyLinks(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAuthResult))

    val res = testController.selectAgentPropertiesSearchSort(PaginationParameters(), GetPropertyLinksParameters(), 1L)(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "agentCodeRadio" -> "1"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText("There are no properties to display")
  }

  private lazy val testController = new AppointAgentController(mockRepresentationConnector, StubGroupAccountConnector, mockPropertyLinkConnector, mockAgentsConnector, StubAuthentication)

  private lazy val mockPropertyLinkConnector = mock[PropertyLinkConnector]

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockAgentsConnector = mock[AgentsConnector]

  private def stubLogin() = {
    val accounts = Accounts(groupAccountGen, individualGen)
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    (accounts.organisation, accounts.person)
  }
}
