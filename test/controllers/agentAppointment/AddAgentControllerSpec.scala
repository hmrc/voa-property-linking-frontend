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

package controllers.agentAppointment

import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation.{AgentAppointmentChangesResponse, AppointNewAgentSession, No, NoProperties, SearchedAgent, SelectedAgent, Start}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import tests.AllMocks
import utils.{HtmlPage, StubWithAppointAgentSessionRefiner}
import org.mockito.Mockito._
import play.api.mvc.Result
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.Future

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
    verifyPageHeading(res, "Agent code")
    verifyBackLink(res, "http://localhost:9542/business-rates-dashboard/home")
  }
  "showStartPage" should "back link is the cached back link" in {
    val backLink = "/some/back/link"
    stubWithAppointAgentSession
      .stubSession(Start(backLink = Some(backLink)), detailedIndividualAccount, groupAccount(false))
    val res = testController.showStartPage()(FakeRequest())
    status(res) shouldBe OK
    verifyPageHeading(res, "Agent code")
    verifyBackLink(res, backLink)
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode is not provided" in {
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.getAgentDetails()(FakeRequest().withFormUrlEncodedBody("agentCode" -> ""))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "Enter a valid agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode does not exist" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any())).thenReturn(Future successful None)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsList)
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.getAgentDetails()(FakeRequest().withFormUrlEncodedBody("agentCode" -> "213414"))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "There is no agent for the provided agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode is too long" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.failed(new BadRequestException("invalid agentCode")))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsList)
    stubWithAppointAgentSession.stubSession(startJourney, detailedIndividualAccount, groupAccount(false))
    val res = testController.getAgentDetails()(FakeRequest().withFormUrlEncodedBody("agentCode" -> "123456789012345"))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "There is no agent for the provided agent code")
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode belongs to an agent that has already been added by this organisation" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future successful Some(agentDetails))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future successful organisationsAgentsList.copy(
        agents = List(agentSummary.copy(representativeCode = agentOrganisation.representativeCode.get))))
    val res = testController.getAgentDetails()(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"${agentOrganisation.representativeCode.get}"))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#agentCode", "This agent has already been added to your account")
  }

  "getAgentDetails" should "return 303 See Other when valid agentCode is provided and agent has not already been added to organisation" in {
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future successful Some(agentDetails))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future successful organisationsAgentsList)
    val res = testController.getAgentDetails()(FakeRequest().withFormUrlEncodedBody("agentCode" -> "11223"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent")
  }

  "isCorrectAgent" should "show the isCorrectAgent page" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))
    val res = testController.isCorrectAgent()(FakeRequest())
    status(res) shouldBe OK
    verifyPageHeading(res, "Is this your agent?")
  }

  "agentSelected" should "return 400 Bad Request when no option is selected" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))
    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> ""))
    status(res) shouldBe BAD_REQUEST
    verifyErrorPage(res, "#isThisYourAgent", "Select yes if this is your agent")
  }

  "agentSelected" should "return 303 See Other and go to start page when user confirms that the presented agent is not their agent" in {
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "false"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/start")
  }

  "agentSelected" should "return 200 Ok and go to the confirmation page if organisation have no authorisations" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    when(
      mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockSessionRepo.get[SearchedAgent](any(), any()))
      .thenReturn(Future.successful(Some(searchedAgent)))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
    status(res) shouldBe OK
    verifyPageHeading(res, "What happens next", "govuk-heading-m")
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageOnePropertyNoExistingAgent page if organisation has only one authorisation and no existing agent" in {
    stubWithAppointAgentSession.stubSession(searchedAgent, detailedIndividualAccount, groupAccount(false))
    when(
      mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockSessionRepo.get[SearchedAgent](any(), any()))
      .thenReturn(Future.successful(Some(searchedAgent)))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
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

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
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

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody("isThisYourAgent" -> "true"))
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
    verifyPageHeading(res, "Should Some Org manage your property?")
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
      "/business-rates-property-linking/my-organisation/appoint-new-agent/checkYourAnswers")
  }

  "multipleProperties" should "return 200 Ok" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))

    val res = testController.multipleProperties()(FakeRequest())
    status(res) shouldBe OK
    verifyPageHeading(res, "Which properties would you like Some Org to manage?")
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

    val res =
      testController.submitMultipleProperties()(FakeRequest().withFormUrlEncodedBody("multipleProperties" -> "all"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint-new-agent/checkYourAnswers")
  }

  "submitMultipleProperties" should "return 303 See Other and redirect to old journey when ChooseFromList is selected" in {
    stubWithAppointAgentSession.stubSession(selectedAgent, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(selectedAgent)))

    val res = testController.submitMultipleProperties()(
      FakeRequest().withFormUrlEncodedBody("multipleProperties" -> "choose_from_list"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/appoint/properties?page=1" +
        "&pageSize=15&agentCode=12345" +
        "&agentAppointed=BOTH" +
        "&backLink=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fappoint-new-agent%2Fmultiple-properties")

  }

  //checkAnswers

  "checkAnswers - single property" should "return 200 Ok" in {
    val data = managingProperty.copy(singleProperty = true)
    stubWithAppointAgentSession.stubSession(data, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(data)))

    val res = testController.checkAnswers()(FakeRequest())
    status(res) shouldBe OK
    val page = Jsoup.parse(contentAsString(res))
    page.getElementById("manage-property-choice").text() shouldBe "Your property"
  }

  "checkAnswers - single property when user selects no" should "return 200 Ok" in {
    val data = managingProperty.copy(singleProperty = true, managingPropertyChoice = No.name)
    stubWithAppointAgentSession.stubSession(data, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(data)))

    val res = testController.checkAnswers()(FakeRequest())
    status(res) shouldBe OK
    val page = Jsoup.parse(contentAsString(res))
    page.getElementById("manage-property-choice").text() shouldBe "No"
  }

  "checkAnswers - multiple properties" should "return 200 Ok" in {
    stubWithAppointAgentSession.stubSession(managingProperty, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(managingProperty)))

    val res = testController.checkAnswers()(FakeRequest())
    status(res) shouldBe OK
    val page = Jsoup.parse(contentAsString(res))
    page.getElementById("manage-property-choice").text() shouldBe "All properties"
  }

  "checkAnswers - multiple properties when user selects no properties" should "return 200 Ok" in {
    val data = managingProperty.copy(singleProperty = false, managingPropertyChoice = NoProperties.name)
    stubWithAppointAgentSession.stubSession(data, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any()))
      .thenReturn(Future.successful(Some(data)))

    val res = testController.checkAnswers()(FakeRequest())
    status(res) shouldBe OK
    val page = Jsoup.parse(contentAsString(res))
    page.getElementById("manage-property-choice").text() shouldBe "No properties"
  }

  "appointAgent" should "return Ok for valid appointmentChange" in {
    when(mockAgentRelationshipService.assignAgent(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController.appointAgent()(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> "123456", "scope" -> "ALL_PROPERTIES"))

    status(res) shouldBe OK
    verifyPageHeading(res, "What happens next", "govuk-heading-m")

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.confirmation.title")} - Valuation Office Agency - GOV.UK")
    html.shouldContainText("You can assign properties to this agent by")
    html.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/agents",
      "managing your agents"
    )
  }

  "appointAgent" should "return 400 Bad Request when invalid form is submitted - scope is missing" in {
    when(mockAgentRelationshipService.assignAgent(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController.appointAgent()(FakeRequest().withFormUrlEncodedBody("agentCode" -> "123456"))

    status(res) shouldBe BAD_REQUEST
  }

  "appointAgent" should "return 400 Bad Request when invalid form is submitted - agentCode is missing" in {
    when(mockAgentRelationshipService.assignAgent(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController.appointAgent()(FakeRequest().withFormUrlEncodedBody("scope" -> "ALL_PROPERTIES"))

    status(res) shouldBe BAD_REQUEST
  }

  private val stubWithAppointAgentSession = new StubWithAppointAgentSessionRefiner(mockSessionRepo)

  private lazy val testController = new AddAgentController(
    mockCustomErrorHandler,
    preAuthenticatedActionBuilders(),
    stubWithAppointAgentSession,
    mockAgentRelationshipService,
    mockSessionRepo,
    startPageView,
    isTheCorrectAgentView,
    agentToManageOnePropertyView,
    agentToManageMultiplePropertiesView,
    checkYourAnswersView,
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

}
