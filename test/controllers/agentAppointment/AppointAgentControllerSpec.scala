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
import connectors.authorisation.Authenticated
import connectors.{AgentsConnector, PropertyRepresentationConnector}
import controllers.VoaPropertyLinkingSpec
import models._
import models.searchApi._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import services.{AgentRelationshipService, AppointRevokeException}
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import utils.{HtmlPage, StubAuthentication, StubGroupAccountConnector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppointAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  val testAgentAccount = GroupAccount(1l, "1", "companyName", 1l, "email@email.com", "123456",
    true, 1l)

  val testAgents = Seq(OwnerAuthAgent(1l,
    testAgentAccount.id,
    "organisationName",
    "APPROVED",
    StartAndContinue,
    StartAndContinue,
    1l))

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


  "getMyOrganisationPropertyLinksWithAgentFiltering" should "show the appoint agent properties page" in {

    stubLogin()
    val testOwnerAuth = OwnerAuthorisation(1l,
      "APPROVED",
      "1111111",
      1l,
      "address",
      "localAuthorityRef",
      testAgents)
    val testOwnerAuthResult = OwnerAuthResult(start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(testOwnerAuth))
    when(mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAuthResult))
    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful())

    StubGroupAccountConnector.stubAccount(testAgentAccount)

    val res = testController.getMyOrganisationPropertyLinksWithAgentFiltering(PaginationParameters(),
      GetPropertyLinksParameters(),
      1L,
      "START_AND_CONTINUE",
      "START_AND_CONTINUE",
      None)(FakeRequest())
    status(res) mustBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#agentPropertiesTableBody")
  }

  "appointAgentSummary" should "show the summary page" in {
    stubLogin()
    StubGroupAccountConnector.stubAccount(testAgentAccount)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any(), any(), any(), any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful())

    val res = testController.appointAgentSummary()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> "StartAndContinue",
    "challengePermission" -> "StartAndContinue",
    "linkIds[]" -> "1"))

    status(res) mustBe OK

  }

  "appointAgentSummary with form errors" should "show the appoint agent properties page" in {
    stubLogin()
    StubGroupAccountConnector.stubAccount(testAgentAccount)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any(), any(), any(), any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful())

    val res = testController.appointAgentSummary()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> "StartAndContinue",
      "challengePermission" -> "StartAndContinue"))

    status(res) mustBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#agentPropertiesTableBody")

  }

  "appointAgentSummary" should "show the appoint agent properties page when an appointment fails" in {
    stubLogin()
    StubGroupAccountConnector.stubAccount(testAgentAccount)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any(), any(), any(), any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.failed(new AppointRevokeException("")))

    val res = testController.appointAgentSummary()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> "StartAndContinue",
      "challengePermission" -> "StartAndContinue",
      "linkIds[]" -> "1"))

    status(res) mustBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#agentPropertiesTableBody")

  }


  "selectAgentPropertiesSearchSort" should "show a list of properties available for removal from a agent" in {
    stubLogin()
    val testOwnerAuth = OwnerAuthorisation(1l,
      "APPROVED",
      "1111111",
      1l,
      "address",
      "localAuthorityRef",
      testAgents)
    val testOwnerAuthResult = OwnerAuthResult(start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(testOwnerAuth))
    val testPagination = AgentPropertiesParameters(1l)

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAuthResult))

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
    val testAgents = Seq(arbitrary[OwnerAuthAgent].sample.get.copy(organisationId = testAgentAccount.id))

    val testOwnerAuth = OwnerAuthorisation(1l,
      "REVOKED",
      "1111111",
      1l,
      "address",
      "localAuthorityRef",
      testAgents)

    val testOwnerAuthResult = OwnerAuthResult(start = 1,
      size = 15,
      filterTotal = 0,
      total = 0,
      authorisations = Seq())
    val testPagination = AgentPropertiesParameters(1l)

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAuthResult))

    val res = testController.selectAgentPropertiesSearchSort(PaginationParameters(), GetPropertyLinksParameters(), 1L)(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "agentCodeRadio" -> "1"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText("There are no properties to display")
  }

  private lazy val testController = new AppointAgentController(
    mockCustomErrorHandler,
    mockRepresentationConnector,
    StubGroupAccountConnector,
    mockAgentsConnector,
    StubAuthentication,
    mockAppointRevokeService,
    mockSessionRepo
  )

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockAgentsConnector = mock[AgentsConnector]

  private lazy val mockSessionRepo = mock[SessionRepo]

  private lazy val  mockAppointRevokeService = mock[AgentRelationshipService]

  private def stubLogin() = {
    val accounts = Accounts(groupAccountGen, individualGen)
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    (accounts.organisation, accounts.person)
  }
}
