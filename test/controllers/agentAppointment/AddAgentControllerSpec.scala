/*
 * Copyright 2023 HM Revenue & Customs
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
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import tests.AllMocks
import uk.gov.hmrc.http.BadRequestException
import utils.StubWithAppointAgentSessionRefiner

import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class AddAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  "start" should "initialise session and redirect to start page" in {
    val res = testController.start()(FakeRequest())
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/start")
  }
  "showStartPage" should "show the appoint new agent start page" in {
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.showStartPage()(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(res, "http://localhost:9542/business-rates-dashboard/home")
  }

  "showStartPage" should "display the correct content in english" in new StartPageTestCase with English {
    doc.title shouldBe "Appoint an agent to your account - Valuation Office Agency - GOV.UK"
    heading shouldBe "Appoint an agent to your account"
    theyCanIntro shouldBe "When you appoint an agent to your account they can act for you. This means they can:"
    theyCanList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "see detailed property information",
      "see Check and Challenge case correspondence such as messages and emails",
      "send Check and Challenge cases",
      "add your properties to your account"
    )
    theyCanInfo shouldBe "They can act for you on the properties you assign to them and the properties they add to your account."
    youCanIntro shouldBe "You can:"
    youCanList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "appoint more than one agent to your account",
      "assign more than one agent to your property",
      "choose the rating lists an agent can act on for you"
    )

    helpLink.text shouldBe "Help with appointing and managing agents"
    helpLink.attr("href") shouldBe "https://www.gov.uk/guidance/appoint-an-agent"
    startNowButton shouldBe "Start now"
  }

  "showStartPage" should "display the correct content in welsh" in new StartPageTestCase with Welsh {
    doc.title shouldBe "Penodi asiant i’ch cyfrif - Valuation Office Agency - GOV.UK"
    heading shouldBe "Penodi asiant i’ch cyfrif"
    theyCanIntro shouldBe "Pan fyddwch yn penodi asiant i’ch cyfrif, bydd yr asiant hwn yn gallu gweithredu ar eich rhan. Mae hyn yn golygu ei fod yn gallu:"
    theyCanList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "gweld gwybodaeth manwl am eiddo",
      "gweld gohebiaeth ynghylch achosion Gwirio a Herio, megis negeseuon ac e-byst",
      "anfon achosion Gwirio a Herio",
      "ychwanegu eich eiddo i’ch cyfrif"
    )
    theyCanInfo shouldBe "Gall yr asiant weithredu ar eiddo rydych yn eu neilltuo iddo, ac ar eiddo y mae’n eu hychwanegu at eich cyfrif."
    youCanIntro shouldBe "Gallwch wneud y canlynol:"
    youCanList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "penodi mwy nag un asiant i’ch cyfrif",
      "neilltuo’ch eiddo i fwy nag un asiant",
      "dewis pa restr ardrethu y gall asiant ei gweithredu ar eich rhan"
    )

    helpLink.text shouldBe "Help gyda phenodi a rheoli asiantau"
    helpLink.attr("href") shouldBe "https://www.gov.uk/guidance/appoint-an-agent"
    startNowButton shouldBe "Dechrau nawr"
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
    val res = testController.getAgentDetails("some/back/link")(FakeRequest().withFormUrlEncodedBody("agentCode" -> ""))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "Enter a valid agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode does not exist" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any())).thenReturn(Future successful None)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsListWithOneAgent)
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res =
      testController.getAgentDetails("some/back/link")(FakeRequest().withFormUrlEncodedBody("agentCode" -> "213414"))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "There is no agent for the provided agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode is too long" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.failed(new BadRequestException("invalid agentCode")))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsListWithOneAgent)
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.getAgentDetails("some/back/link")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> "123456789012345"))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "There is no agent for the provided agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode belongs to an agent that has already been added by this organisation" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future successful Some(agentDetails))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future successful organisationsAgentsListWithOneAgent.copy(
        agents = List(agentSummary.copy(representativeCode = agentOrganisation.representativeCode.get))))
    val res = testController.getAgentDetails("some/back/link")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"${agentOrganisation.representativeCode.get}"))
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
      testController.getAgentDetails("some/back/link")(FakeRequest().withFormUrlEncodedBody("agentCode" -> "11223"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent")
  }

  "isThisYourAgentPage" should "go back to the agent code page" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    val res = testController.isCorrectAgent()(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(
      res,
      "/business-rates-property-linking/my-organisation/appoint-new-agent/agent-code?backLink=http%3A%2F%2Flocalhost%3A9542%2Fbusiness-rates-dashboard%2Fhome"
    )
  }

  "isThisYourAgentPage" should "display the correct content in english" in new IsThisYourAgentPageTestCase
  with English {
    doc.title shouldBe "Is this your agent? - Valuation Office Agency - GOV.UK"
    caption shouldBe "Appoint an agent"
    heading shouldBe "Is this your agent?"
    agentDetails shouldBe "Some Org AN ADDRESS"
    radioLabels.asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Yes",
      "No, enter a new code"
    )
    continueButton shouldBe "Continue"
  }
  "isThisYourAgentPage" should "display the correct content in welsh" in new IsThisYourAgentPageTestCase with Welsh {
    doc.title shouldBe "Ai’ch asiant chi yw hwn? - Valuation Office Agency - GOV.UK"
    caption shouldBe "Penodi asiant"
    heading shouldBe "Ai’ch asiant chi yw hwn?"
    agentDetails shouldBe "Some Org AN ADDRESS"
    radioLabels.asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Iawn",
      "Na, nodwch god newydd"
    )
    continueButton shouldBe "Yn eich blaen"
  }

  "agentSelected" should "return 400 Bad Request when no option is selected" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))
    val res =
      testController.agentSelected("some/back/link")(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> ""))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#isThisYourAgent", "Select yes if this is your agent")
  }

  "agentSelected" should "return 303 See Other and go to start page when user confirms that the presented agent is not their agent" in {
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res =
      testController.agentSelected("some/back/link")(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "false"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/agent-code")
  }

  "agentSelected" should "return 303 Ok and go to the check your answers page if organisation have no authorisations" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    when(
      mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockSessionRepo.get[SearchedAgent](any(), any()))
      .thenReturn(Future.successful(Some(searchedAgent)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))
    when(mockAgentRelationshipService.postAgentAppointmentChange(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res =
      testController.agentSelected("some/back/link")(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageOnePropertyNoExistingAgent page if organisation has only one authorisation and no existing agent" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    when(
      mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockSessionRepo.get[SearchedAgent](any(), any()))
      .thenReturn(Future.successful(Some(searchedAgent)))

    val res =
      testController.agentSelected("some/back/link")(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property")
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageOneProperty page if organisation has only one authorisation and an existing agent" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    when(
      mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockSessionRepo.get[SearchedAgent](any(), any()))
      .thenReturn(Future.successful(Some(searchedAgent)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res =
      testController.agentSelected("some/back/link")(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property")
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageMultipleProperties page if organisation has only multiple authorisations" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    when(
      mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockSessionRepo.get[SearchedAgent](any(), any()))
      .thenReturn(Future.successful(Some(searchedAgent)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res =
      testController.agentSelected("some/back/link")(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties")
  }

  "oneProperty" should "return 200 Ok" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))

    val res = testController.oneProperty()(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(res, "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent")
  }

  "oneProperty" should "return 200 Ok with correct back link when CYA page has been visited" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))

    val res = testController.oneProperty(true)(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(res, "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
  }

  "submitOneProperty" should "return 400 Bad Request if no selection is made" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.submitOneProperty()(FakeRequest().withFormUrlEncodedBody("oneProperty" -> ""))

    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#oneProperty", "Select if you want your agent to manage your property")
  }

  "submitOneProperty" should "return 303 See Other when valid selection is made" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[SelectedAgent](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res =
      testController.submitOneProperty()(FakeRequest().withFormUrlEncodedBody("oneProperty" -> "no"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
  }

  "multipleProperties" should "return 200 Ok" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))

    val res = testController.multipleProperties()(FakeRequest())
    status(res) shouldBe OK
    verifyPageHeading(res, "Which of your properties do you want to assign Some Org to?")
  }

  "multipleProperties" should "back link should return to CYA page" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))

    val res = testController.multipleProperties(true)(FakeRequest())
    status(res) shouldBe OK
    verifyBackLink(res, "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
  }

  "submitMultipleProperties" should "return 400 Bad Request if no selection is made" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))

    val res =
      testController.submitMultipleProperties()(FakeRequest().withFormUrlEncodedBody("multipleProperties" -> ""))

    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#multipleProperties", "Select if you want your agent to manage any of your properties")
  }

  "submitMultipleProperties" should "return 303 See Other when valid selection is made" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any()))
      .thenReturn(Future.successful(1))

    val res =
      testController.submitMultipleProperties()(FakeRequest().withFormUrlEncodedBody("multipleProperties" -> "all"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
  }

  "submitMultipleProperties" should "return 303 See Other and redirect to old journey when ChooseFromList is selected" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any()))
      .thenReturn(Future.successful(1))

    val res = testController.submitMultipleProperties()(
      FakeRequest().withFormUrlEncodedBody("multipleProperties" -> "choose_from_list"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint/properties?page=1" +
        "&pageSize=15&agentCode=12345" +
        "&agentAppointed=BOTH" +
        "&backLink=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fappoint-new-agent%2Fmultiple-properties&fromManageAgentJourney=false")

  }

  private val stubWithAppointAgentSession = new StubWithAppointAgentSessionRefiner(mockSessionRepo)

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
    agentToManageMultiplePropertiesView,
    addAgentconfirmationView
  )

  private lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  private def verifyPageHeading(
        res: Future[Result],
        expectedHeading: String,
        headingClass: String = "govuk-heading-l") = {
    val page = Jsoup.parse(contentAsString(res))
    page.getElementsByAttributeValue("class", headingClass).text() shouldBe expectedHeading
  }

  private def verifyErrorPage(res: Future[Result], errorHref: String, expectedErrorMessage: String) = {
    val page = Jsoup.parse(contentAsString(res))
    page.getElementsByTag("title").text().startsWith("Error:") shouldBe true
    page.getElementsByAttributeValue("href", errorHref).text() shouldBe expectedErrorMessage
  }

  private def verifyBackLink(res: Future[Result], expectedBackLink: String) = {
    val page = Jsoup.parse(contentAsString(res))
    page.getElementById("back-link").attr("href") shouldBe expectedBackLink
  }

  type English = EnglishRequest
  type Welsh = WelshRequest

  trait StartPageTestCase {
    self: RequestLang =>
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))

    val doc: Document = Jsoup.parse(contentAsString(testController.showStartPage()(self.fakeRequest)))
    val heading: String = doc.getElementsByTag("h1").text
    val theyCanIntro: String = doc.getElementById("they-can-intro").text
    val theyCanList: Element = doc.getElementById("they-can-list")
    val theyCanInfo: String = doc.getElementById("they-can-info").text
    val youCanIntro: String = doc.getElementById("you-can-intro").text
    val youCanList: Element = doc.getElementById("you-can-list")
    val helpLink: Element = doc.getElementById("help-link")
    val startNowButton: String = doc.getElementById("start-now-button").text
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

  trait ConfirmationPageTestCase {
    self: RequestLang =>
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))

    val doc: Document = Jsoup.parse(contentAsString(testController.confirmAppointAgent()(FakeRequest())))
    val heading: String = doc.getElementsByTag("h1").text
    val dynamicPropertiesAssignedToText: String = doc.getElementById("assigned-to").text
    val agentCanText: String = doc.getElementById("agent-can-text").text
    val agentCanList: Element = doc.getElementById("agent-can-list")
    val whatHappensNextTitle: String = doc.getElementById("what-happens-next-title").text
    val whatHappensNextText: String = doc.getElementById("what-happens-next-text").text
    val manageAgentsLink = doc.getElementById("manage-agents-link")
    val homeLink: Element = doc.getElementById("go-home-link")
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
