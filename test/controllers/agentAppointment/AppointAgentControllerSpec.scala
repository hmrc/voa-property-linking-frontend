/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{AgentsConnector, Authenticated, PropertyRepresentationConnector}
import controllers.VoaPropertyLinkingSpec
import models._
import models.searchApi.{OwnerAgent, OwnerAgents, OwnerAuthResult, OwnerAuthorisation}
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

  "selectProperties" should "show a list of properties available for appointment to an agent" in {
    stubLogin()
    val testAgentAccount = arbitrary[GroupAccount].sample.get.copy(isAgent = true, agentCode = 1l)
    val testOwnerAuth = arbitrary[OwnerAuthorisation].sample.get.copy(agents = None)
    val testOwnerAuthResult = OwnerAuthResult(start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(testOwnerAuth))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockPropertyLinkConnector.appointableProperties(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(testOwnerAuthResult))

    val res = testController.selectProperties()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "agentCodeRadio" -> "1",
      "canCheck" -> StartAndContinue.name,
      "canChallenge" -> StartAndContinue.name
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(testOwnerAuth.address)
  }


  "appointAgentSummary" should "perform the bulk agent appointment action and display the summary page for checks only" in {
    stubLogin()
    val testAgentAccount = arbitrary[GroupAccount].sample.get.copy(isAgent = true, agentCode = 1l)
    val testPropertyLink = arbitrary[PropertyLink].sample.get

    StubGroupAccountConnector.stubAccount(testAgentAccount)

    when(mockPropertyLinkConnector.get(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testPropertyLink)))

    val res = testController.appointAgentSummary()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> StartAndContinue.name,
      "challengePermission" -> NotPermitted.name,
      "linkIds[]" -> "123"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(s"You have appointed agent '${testAgentAccount.companyName}' (${testAgentAccount.agentCode}) with the ability to submit checks to the selected properties.")
  }

  "appointAgentSummary" should "perform the bulk agent appointment action and display the summary page for challenges only" in {
    stubLogin()
    val testAgentAccount = arbitrary[GroupAccount].sample.get.copy(isAgent = true, agentCode = 1l)
    val testPropertyLink = arbitrary[PropertyLink].sample.get

    StubGroupAccountConnector.stubAccount(testAgentAccount)

    when(mockPropertyLinkConnector.get(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testPropertyLink)))

    val res = testController.appointAgentSummary()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> NotPermitted.name,
      "challengePermission" -> StartAndContinue.name,
      "linkIds[]" -> "123"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(s"You have appointed agent '${testAgentAccount.companyName}' (${testAgentAccount.agentCode}) with the ability to submit challenges to the selected properties.")
  }

  "appointAgentSummary" should "perform the bulk agent appointment action and display the summary page for checks and challenges" in {
    stubLogin()
    val testAgentAccount = arbitrary[GroupAccount].sample.get.copy(isAgent = true, agentCode = 1l)
    val testPropertyLink = arbitrary[PropertyLink].sample.get

    StubGroupAccountConnector.stubAccount(testAgentAccount)

    when(mockPropertyLinkConnector.get(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testPropertyLink)))

    val res = testController.appointAgentSummary()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> StartAndContinue.name,
      "challengePermission" -> StartAndContinue.name,
      "linkIds[]" -> "123"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(s"You have appointed agent '${testAgentAccount.companyName}' (${testAgentAccount.agentCode}) with the ability to submit checks and challenges to the selected properties.")
  }

  "createAndSubmitAgentRepRequest" should "submit an agent representation request" in {
    stubLogin()

    when(mockPropertyLinkConnector.get(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testPropertyLink)))

    val res = testController.createAn()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "1",
      "checkPermission" -> StartAndContinue.name,
      "challengePermission" -> StartAndContinue.name,
      "linkIds[]" -> "123"
    ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(s"You have appointed agent '${testAgentAccount.companyName}' (${testAgentAccount.agentCode}) with the ability to submit checks and challenges to the selected properties.")
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
