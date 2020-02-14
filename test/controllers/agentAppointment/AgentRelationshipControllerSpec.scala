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

package controllers.agentAppointment

import connectors.AgentsConnector
import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation.AppointNewAgentSession
import org.mockito.ArgumentMatchers.any
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import tests.AllMocks
import utils.StubWithAppointAgentSession
import org.mockito.Mockito._
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentRelationshipControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  "startAppointJourney" should "show the appoint new agent start page" in {
    when(startPage.apply(any())(any(),any(),any())).thenReturn(Html(""))
    val res = testController.startAppointJourney()(FakeRequest())
    status(res) mustBe OK
  }
  "getAgentDetails" should "return 400 Bad Request when agentCode is not provided" in {
    when(startPage.apply(any())(any(),any(),any())).thenReturn(Html(""))
    val res = testController.getAgentDetails()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> ""))
    status(res) mustBe BAD_REQUEST
  }

  "getAgentDetails" should "return 400 Bad Request when agentCode is provided but there is no Agent for the provided agentCode" in {
    when(startPage.apply(any())(any(),any(),any())).thenReturn(Html(""))
    when(mockAgentRelationshipService.getAgent(any())(any())).thenReturn(Future successful Some(agentOrganisation))
    val res = testController.getAgentDetails()(FakeRequest().withFormUrlEncodedBody(
      "agentCode" -> "11223"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent")
  }

  "isCorrectAgent" should "show the isCorrectAgent page" in {
    when(isTheCorrectAgent.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))
    val res = testController.isCorrectAgent()(FakeRequest())
    status(res) mustBe OK
  }

  "agentSelected" should "return 400 Bad Request when no option is selected" in {
    when(isTheCorrectAgent.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))
    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody(
      "isThisYourAgent" -> ""))
    status(res) mustBe BAD_REQUEST
  }

  "agentSelected" should "return 303 See Other and go to start page when user confirms that the presented agent is not their agent" in {
    when(confirmation.apply(any())(any(),any(),any())).thenReturn(Html(""))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody(
      "isThisYourAgent" -> "false"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent")
  }

  "agentSelected" should "return 200 Ok and go to the confirmation page if organisation have no authorisations" in {
    when(confirmation.apply(any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockAgentsConnector.ownerAgents(any())(any())).thenReturn(Future.successful(ownerAgentsNoAgents))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody(
      "isThisYourAgent" -> "true"))
    status(res) mustBe OK
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageOnePropertyNoExistingAgent page if organisation has only one authorisation and no existing agent" in {
    when(agentToManageOnePropertyNoExistingAgent.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockAgentsConnector.ownerAgents(any())(any())).thenReturn(Future.successful(ownerAgentsNoAgents))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody(
      "isThisYourAgent" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/one-property/no-existing-agent")
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageOneProperty page if organisation has only one authorisation and an existing agent" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockAgentsConnector.ownerAgents(any())(any())).thenReturn(Future.successful(ownerAgentsWithOneAgent))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody(
      "isThisYourAgent" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/one-property/with-existing-agent")
  }

  "agentSelected" should "return 303 See Other and go to the agentToManageMultipleProperties page if organisation has only multiple authorisations" in {
    when(agentToManageMultipleProperties.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentsConnector.ownerAgents(any())(any())).thenReturn(Future.successful(ownerAgentsWithOneAgent))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.agentSelected()(FakeRequest().withFormUrlEncodedBody(
      "isThisYourAgent" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties")
  }

  "onePropertyNoExistingAgent" should "return 200 Ok" in {
    when(agentToManageOnePropertyNoExistingAgent.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.onePropertyNoExistingAgent()(FakeRequest())
    status(res) mustBe OK
  }

  "submitOnePropertyNoExistingAgent" should "return 400 Bad Request if no selection is made" in {
    when(agentToManageOnePropertyNoExistingAgent.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitOnePropertyNoExistingAgent()(FakeRequest().withFormUrlEncodedBody(
      "onePropertyNoAgent" -> ""))

    status(res) mustBe BAD_REQUEST
  }

  "submitOnePropertyNoExistingAgent" should "return 303 See Other when valid selection is made" in {
    when(agentToManageOnePropertyNoExistingAgent.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitOnePropertyNoExistingAgent()(FakeRequest().withFormUrlEncodedBody(
      "onePropertyNoAgent" -> "true"))

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/checkYourAnswers")
  }

  "onePropertyWithExistingAgent" should "return 200 Ok" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.onePropertyWithExistingAgent()(FakeRequest())
    status(res) mustBe OK
  }

  "submitOnePropertyWithExistingAgent" should "return 400 Bad Request if no selection is made" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitOnePropertyWithExistingAgent()(FakeRequest().withFormUrlEncodedBody(
      "onePropertyWithAgent" -> ""))

    status(res) mustBe BAD_REQUEST
  }

  "submitOnePropertyWithExistingAgent" should "return 303 See Other when valid selection is made" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitOnePropertyWithExistingAgent()(FakeRequest().withFormUrlEncodedBody(
      "onePropertyWithAgent" -> "AS_WELL_AS_CURRENT"))

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/checkYourAnswers")
  }

  "multipleProperties" should "return 200 Ok" in {
    when(agentToManageMultipleProperties.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.multipleProperties()(FakeRequest())
    status(res) mustBe OK
  }

  "submitMultipleProperties" should "return 400 Bad Request if no selection is made" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitMultipleProperties()(FakeRequest().withFormUrlEncodedBody(
      "multipleProperties" -> ""))

    status(res) mustBe BAD_REQUEST
  }

  "submitMultipleProperties" should "return 303 See Other when valid selection is made" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitMultipleProperties()(FakeRequest().withFormUrlEncodedBody(
      "multipleProperties" -> "ALL"))

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/checkYourAnswers")
  }

  "submitMultipleProperties" should "return 303 See Other and redirect to old journey when ChooseFromList is selected" in {
    when(agentToManageOneProperty.apply(any(), any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.submitMultipleProperties()(FakeRequest().withFormUrlEncodedBody(
      "multipleProperties" -> "CHOOSE_FROM_LIST"))

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&sortfield=ADDRESS&sortorder=ASC&agentCode=12345&checkPermission=START_AND_CONTINUE&challengePermission=START_AND_CONTINUE&agentAppointed=BOTH")
  }

  //checkAnswers

  "checkAnswers" should "return 200 Ok" in {
    when(checkYourAnswers.apply(any())(any(),any(),any())).thenReturn(Html(""))
    stubWithAppointAgentSession.stubSession(appointNewAgentSession, detailedIndividualAccount, groupAccount(false))
    when(mockSessionRepo.get[AppointNewAgentSession](any(), any())).thenReturn(Future.successful(Some(appointNewAgentSession)))

    val res = testController.checkAnswers()(FakeRequest())
    status(res) mustBe OK
  }

  private val stubWithAppointAgentSession = new StubWithAppointAgentSession(mockSessionRepo)

  private lazy val testController = new AgentRelationshipController(
    mockCustomErrorHandler,
    preAuthenticatedActionBuilders(),
    stubWithAppointAgentSession,
    mockAgentRelationshipService,
    mockAgentsConnector,
    mockSessionRepo,
    startPage,
    isTheCorrectAgent,
    agentToManageOnePropertyNoExistingAgent,
    agentToManageOneProperty,
    agentToManageMultipleProperties,
    checkYourAnswers,
    confirmation
  )

  private lazy val mockAgentsConnector = mock[AgentsConnector]

  private lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }

  private lazy val startPage = mock[views.html.propertyrepresentation.appoint.start]
  private lazy val isTheCorrectAgent = mock[views.html.propertyrepresentation.appoint.isThisYourAgent]
  private lazy val agentToManageOnePropertyNoExistingAgent = mock[views.html.propertyrepresentation.appoint.agentToManageOnePropertyNoExistingAgent]
  private lazy val agentToManageOneProperty = mock[views.html.propertyrepresentation.appoint.agentToManageOneProperty]
  private lazy val agentToManageMultipleProperties = mock[views.html.propertyrepresentation.appoint.agentToManageMultipleProperties]
  private lazy val checkYourAnswers = mock[views.html.propertyrepresentation.appoint.checkYourAnswers]
  private lazy val confirmation = mock[views.html.propertyrepresentation.appoint.confirmation]

}
