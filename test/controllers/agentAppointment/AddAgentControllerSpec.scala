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

package controllers.agentAppointment

import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import tests.AllMocks
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.StubWithAppointAgentSessionRefiner

import scala.concurrent.Future

class AddAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  "start" should "initialise session and redirect to start page" in {
    val res = testController.start()(FakeRequest())
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/start")
  }

  "showAgentCodePage" should "show the agent code page and go back to the appoint agent start page" in {
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))

    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(None))

    val res = testController.showAgentCodePage()(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(res, "/business-rates-property-linking/my-organisation/appoint-new-agent/start")
  }

  "showAgentCodePage" should "back link to the check your answers page when its been visited" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))

    val res = testController.showAgentCodePage(true)(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(res, "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
  }
  "showAgentCodePage" should "display the correct content in english" in new AgentCodePageTestCase with English {
    doc.title shouldBe "What is your agent’s code? - Valuation Office Agency - GOV.UK"
    caption shouldBe "Appoint an agent"
    heading shouldBe "What is your agent’s code?"
    agentCodeHint shouldBe "This is a number given to the agent by the Valuation Office Agency."
    continueButton shouldBe "Continue"
  }
  "showAgentCodePage" should "display the correct content in welsh" in new AgentCodePageTestCase with Welsh {
    doc.title shouldBe "Beth yw cod eich asiant? - Valuation Office Agency - GOV.UK"
    caption shouldBe "Penodi asiant"
    heading shouldBe "Beth yw cod eich asiant?"
    agentCodeHint shouldBe "Dyma’r rhif a roddir i’r asiant gan Asiantaeth y Swyddfa Brisio."
    continueButton shouldBe "Yn eich blaen"
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode is not provided" in {
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.getAgentDetails(RedirectUrl("http://localhost/some-back-link"))(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> "")
    )
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "Enter a valid agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode does not exist" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any())).thenReturn(Future successful None)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsListWithOneAgent)
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res =
      testController.getAgentDetails(RedirectUrl("http://localhost/some-back-link"))(
        FakeRequest().withFormUrlEncodedBody("agentCode" -> "213414")
      )
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "There is no agent for the provided agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode is too long" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.failed(new BadRequestException("invalid agentCode")))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsListWithOneAgent)
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.getAgentDetails(RedirectUrl("http://localhost/some-back-link"))(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> "123456789012345")
    )
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "There is no agent for the provided agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode belongs to an agent that has already been added by this organisation" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future successful Some(agentDetails))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future successful organisationsAgentsListWithOneAgent
        .copy(agents = List(agentSummary.copy(representativeCode = agentOrganisation.representativeCode.get)))
    )
    val res = testController.getAgentDetails(RedirectUrl("http://localhost/some-back-link"))(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"${agentOrganisation.representativeCode.get}")
    )
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "This agent has already been added to your account")
  }

  "getAgentDetails" should "return 303 See Other when valid agentCode is provided and agent has not already been added to organisation" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.unit)
    val res =
      testController.getAgentDetails(RedirectUrl("http://localhost/some-back-link"))(
        FakeRequest().withFormUrlEncodedBody("agentCode" -> "11223")
      )
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent"
    )
  }

  type English = EnglishRequest
  type Welsh = WelshRequest
  private lazy val testController = new AddAgentController(
    mockCustomErrorHandler,
    preAuthenticatedActionBuilders(),
    stubWithAppointAgentSession,
    mockAgentRelationshipService,
    mockSessionRepo,
    startPageView,
    agentCodePageView,
    isTheCorrectAgentView,
    agentToManageOnePropertyView,
    agentToManageMultiplePropertiesView
  )
  private lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }
  private val stubWithAppointAgentSession = new StubWithAppointAgentSessionRefiner(mockSessionRepo)

  private def verifyErrorPage(res: Future[Result], errorHref: String, expectedErrorMessage: String) = {
    val page = Jsoup.parse(contentAsString(res))
    page.getElementsByTag("title").text().startsWith("Error:") shouldBe true
    page.getElementsByAttributeValue("href", errorHref).text() shouldBe expectedErrorMessage
  }

  private def verifyBackLink(res: Future[Result], expectedBackLink: String) = {
    val page = Jsoup.parse(contentAsString(res))
    page.getElementById("back-link").attr("href") shouldBe expectedBackLink
  }

  trait AgentCodePageTestCase {
    self: RequestLang =>
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))

    val doc: Document = Jsoup.parse(contentAsString(testController.showAgentCodePage()(self.fakeRequest)))
    val caption: String = doc.getElementById("caption").text()
    val heading: String = doc.getElementsByTag("h1").text
    val agentCodeHint: String = doc.getElementById("agentCode-hint").text
    val continueButton: String = doc.getElementById("continue-button").text()
  }

  trait IsThisYourAgentPageTestCase {
    self: RequestLang =>
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))

    val doc: Document = Jsoup.parse(contentAsString(testController.isCorrectAgent()(self.fakeRequest)))
    val caption: String = doc.getElementById("caption").text()
    val heading: String = doc.getElementsByTag("h1").text
    val agentDetails: String = doc.getElementById("agent-details").text
    val radioLabels = doc.getElementsByTag("label")
    val continueButton: String = doc.getElementById("continue-button").text
  }

}
