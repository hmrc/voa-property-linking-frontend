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

package controllers.agent

import actions.AuthenticatedAction
import config.ApplicationConfig
import controllers.VoaPropertyLinkingSpec
import controllers.agent.ManageAgentController
import models.propertyrepresentation.{AppointAgentResponse, AppointmentScope, AssignToAllProperties}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import play.twirl.api.Html
import services.AgentRelationshipService
import tests.AllMocks
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class ManageAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  private val mockManageAgentPage = mock[views.html.propertyrepresentation.manage.manageAgent]
  private val mockRemoveAgentFromOrganisationPage =
    mock[views.html.propertyrepresentation.manage.removeAgentFromOrganisation]
  private val mockUnassignAgentFromPropertyPage =
    mock[views.html.propertyrepresentation.manage.unassignAgentFromProperty]
  private val mockAddAgentToAllPropertyPage =
    mock[views.html.propertyrepresentation.manage.addAgentToAllProperty]
  private val mockConfirmAddAgentToAllProperty =
    mock[views.html.propertyrepresentation.manage.confirmAddAgentToAllProperty]

  val testController = new ManageAgentController(
    errorHandler = mockCustomErrorHandler,
    authenticated = preAuthenticatedActionBuilders(userIsAgent = false),
    agentRelationshipService = mockAgentRelationshipService,
    manageAgentPage = mockManageAgentPage,
    removeAgentFromOrganisation = mockRemoveAgentFromOrganisationPage,
    unassignAgentFromProperty = mockUnassignAgentFromPropertyPage,
    addAgentToAllProperty = mockAddAgentToAllPropertyPage,
    confirmAddAgentToAllProperty = mockConfirmAddAgentToAllProperty
  )

  "manageAgent" should "show the manage agent page" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockRemoveAgentFromOrganisationPage.apply(any(), any())(any(), any(), any())).thenReturn(Html(""))
    val res = testController.manageAgent(None)(FakeRequest())
    status(res) mustBe OK
  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent but no property links" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockRemoveAgentFromOrganisationPage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has Zero PropertyLinks"))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has Zero PropertyLinks"

  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent and one property link (agent not assigned)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockManageAgentPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has One PropertyLink - agent not assigned"))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has One PropertyLink - agent not assigned"
  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent and one property link (agent assigned)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 1)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockUnassignAgentFromPropertyPage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has One PropertyLink - agent assigned"))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has One PropertyLink - agent assigned"
  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent and one property link (agent assigned) - agentCodeProvided" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockUnassignAgentFromPropertyPage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has One PropertyLink - agent assigned"))

    val res = testController.getManageAgentPage(Some(agentCode))(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has One PropertyLink - agent assigned"
  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent and more than one property links (agent not assigned)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockManageAgentPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has more than one PropertyLinks - agent not assigned"))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has more than one PropertyLinks - agent not assigned"
  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent and more than one property links (agent assigned to some)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 1)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockManageAgentPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has more than one PropertyLinks - agent assigned to some"))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has more than one PropertyLinks - agent assigned to some"
  }

  "getManageAgentPage" should "return the correct manage agent page when org has one agent and more than one property links (agent assigned to all)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 2)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockManageAgentPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has more than one PropertyLinks - agent assigned to all"))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue.isDefined mustBe true
    res.futureValue.get.toString() mustBe "IP has more than one PropertyLinks - agent assigned to all"
  }

  "getManageAgentPage" should "return None for an invalid propertyLinks and agent details combination" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 9)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentPage(None)(FakeRequest())
    res.futureValue mustBe None
  }

  "submitManageAgent" should "return 400 Bad Request when an invalid manage property option is submitted " in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockUnassignAgentFromPropertyPage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has One PropertyLink - agent assigned"))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest().withFormUrlEncodedBody("manageAgentOption" -> "BLAH", "agentCode" -> s"$agentCode"))
    status(res) mustBe BAD_REQUEST
  }

  "submitManageAgent" should "return 400 Bad Request when agentCode is not submitted " in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))
    when(mockUnassignAgentFromPropertyPage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has One PropertyLink - agent assigned"))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest().withFormUrlEncodedBody("manageAgentOption" -> "unassignFromAllProperties"))
    status(res) mustBe BAD_REQUEST
  }

  "submitManageAgent" should "return 200 Ok when IP chooses to appoint agent to all properties" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))
    when(mockManageAgentPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("IP has more than one PropertyLink - agent assigned to some"))
    when(mockAddAgentToAllPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("addAgentToAllProperty"))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${AssignToAllProperties.name}", "agentName" -> "Agent Org"))

    status(res) mustBe OK
  }

  "assignAgentToAll" should "return 400 Bad Request when invalid form submitted" in {
    when(mockAddAgentToAllPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("addAgentToAllProperty"))

    val res = testController.assignAgentToAll(agentCode, "Some agent org")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) mustBe BAD_REQUEST
  }

  "assignAgentToAll" should "return 200 Ok a valid form is submitted" in {
    when(mockAgentRelationshipService.sendAppointAgentRequest(any())(any()))
      .thenReturn(Future.successful(AppointAgentResponse("some-id")))
    when(mockConfirmAddAgentToAllProperty.apply(any())(any(), any(), any()))
      .thenReturn(Html("confirmAddAgentToAllProperty"))

    val res = testController.assignAgentToAll(agentCode, "Some agent org")(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.ALL_PROPERTIES}"))

    status(res) mustBe OK
  }

}
