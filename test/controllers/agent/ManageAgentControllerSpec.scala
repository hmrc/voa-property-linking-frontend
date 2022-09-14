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

package controllers.agent

import binders.propertylinks.GetPropertyLinksParameters
import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.AllMocks
import utils.HtmlPage

import scala.concurrent.Future

class ManageAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  lazy val testController = new ManageAgentController(
    errorHandler = mockCustomErrorHandler,
    authenticated = preAuthenticatedActionBuilders(userIsAgent = false),
    agentRelationshipService = mockAgentRelationshipService,
    manageAgentView = manageAgentView,
    myAgentsView = myAgentsView,
    removeAgentFromOrganisationView = removeAgentFromOrganisationView,
    unassignAgentFromPropertyView = unassignAgentFromPropertyView,
    addAgentToAllPropertiesView = addAgentToAllPropertyView,
    confirmAddAgentToAllPropertiesView = confirmAddAgentToAllPropertyView,
    unassignAgentFromAllPropertiesView = unassignAgentFromAllPropertiesView,
    confirmUnassignAgentFromAllPropertiesView = confirmUnassignAgentFromAllPropertiesView,
    confirmRemoveAgentFromOrganisationView = confirmRemoveAgentFromOrganisationView,
    manageAgentPropertiesView = manageAgentPropertiesView
  )

  "showAgents" should "show the manage agent page" in {
    val propertyLinksCount = 1
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any()))
      .thenReturn(Future.successful(propertyLinksCount))
    val res = testController.showAgents()(FakeRequest())
    status(res) shouldBe OK

    val returnedDocument = Jsoup.parse(contentAsString(res))
    Option(returnedDocument.getElementById("add-agent-link")).map(_.text()) shouldBe Some("Appoint an agent")
  }

  "showAgents" should "show 'You have no Agents' text on agents page" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(AgentList(0, List.empty)))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any()))
      .thenReturn(Future.successful(0))

    val res = testController.showAgents()(FakeRequest())
    status(res) shouldBe OK

    val returnedDocument = Jsoup.parse(contentAsString(res))
    returnedDocument.getElementById("no-agents").text() shouldBe "You have no agents."
  }

  "manageAgent" should "show the manage agent page" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))

    val res = testController.manageAgent(None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.removeFromAccount.title")} - Valuation Office Agency - GOV.UK")
  }

  "manageAgentProperties" should "return the correct manage agent page with property links" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))

    val res = testController.manageAgentProperties(agentCode, GetPropertyLinksParameters(), None, None, None, None)(
      FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.agentProperties.assigned.title")} - Valuation Office Agency - GOV.UK")

    html.html
      .getElementById("back-link")
      .attr("href") shouldBe controllers.agent.routes.ManageAgentController.showAgents.url

  }

  "manageAgentProperties" should "return the correct back  link for provided parameters - propertyLinkId, valuationId, propertyLinkSubmissionId" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))

    val res = testController
      .manageAgentProperties(agentCode, GetPropertyLinksParameters(), Some(1L), Some(1L), Some("subId"), None)(
        FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.html
      .getElementById("back-link")
      .attr("href") shouldBe "http://localhost:9537/business-rates-valuation/property-link/1/valuations/1?submissionId=subId#agents-tab"

  }

  "manageAgentProperties" should "return the correct back  link for provided parameters - valuationId, propertyLinkSubmissionId, uarn" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))

    val res = testController
      .manageAgentProperties(agentCode, GetPropertyLinksParameters(), None, Some(1L), Some("subId"), Some(1L))(
        FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.html
      .getElementById("back-link")
      .attr("href") shouldBe s"${controllers.detailedvaluationrequest.routes.DvrController
      .myOrganisationRequestDetailValuationCheck(propertyLinkSubmissionId = "subId", valuationId = 1L, uarn = 1L)
      .url}#agents-tab"

  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent but no property links" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))

    val res = testController.getManageAgentView(None)(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.removeFromAccount.title")} - Valuation Office Agency - GOV.UK")

  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and one property link (agent not assigned)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView(None)(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and one property link (agent assigned)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 1)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView(None)(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.unassignFromProperty.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and one property link (agent assigned) - agentCodeProvided" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView(Some(agentCode))(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.unassignFromProperty.title")} - Valuation Office Agency - GOV.UK")

  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and more than one property links (agent not assigned)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView(None)(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and more than one property links (agent assigned to some)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 1)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentView(None)(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and more than one property links (agent assigned to all)" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 2)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentView(None)(FakeRequest()).futureValue.get
    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return None for an invalid propertyLinks and agent details combination" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsList.copy(agents = List(agentSummary.copy(propertyCount = 9)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentView(None)(FakeRequest())
    res.futureValue shouldBe None
  }

  "submitManageAgent" should "return 400 Bad Request when an invalid manage property option is submitted " in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest().withFormUrlEncodedBody("manageAgentOption" -> "BLAH", "agentCode" -> s"$agentCode"))
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.unassignFromProperty.title")} - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 400 Bad Request when agentCode is not submitted " in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest().withFormUrlEncodedBody("manageAgentOption" -> "unassignFromAllProperties"))
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.unassignFromProperty.title")} - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 200 Ok when IP chooses to appoint agent to all properties" in {
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${AssignToAllProperties.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Confirm you want to assign agent to all properties - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 303 Redirect when IP chooses to appoint agent to some properties" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${AssignToSomeProperties.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe
      Some(
        "/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&" +
          "agentCode=12345&agentAppointed=BOTH&backLink=%2F" +
          "business-rates-property-linking%2Fmy-organisation%2Fmanage-agent%3FagentCode%3D12345")
  }

  "submitManageAgent" should "return 200 Ok when IP chooses to unassigned agent from all properties" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(FakeRequest()
      .withFormUrlEncodedBody("manageAgentOption" -> s"${UnassignFromAllProperties.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Confirm you want to unassign agent from all properties - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 303 Redirect when IP chooses to unassign agent from some properties" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(FakeRequest()
      .withFormUrlEncodedBody("manageAgentOption" -> s"${UnassignFromSomeProperties.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe
      Some(
        "/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15" +
          "&agentCode=12345")

  }

  "submitManageAgent" should "return 200 Ok when IP chooses to remove agent from account" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${RemoveFromYourAccount.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Remove agent from your account - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 200 Ok when IP chooses to appoint agent to only property" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsList.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${AssignToYourProperty.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Confirm you want to assign agent to all properties - Valuation Office Agency - GOV.UK")
  }

  "assignAgentToAll" should "return 400 Bad Request when invalid form submitted" in {
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.assignAgentToAll(agentCode, "Some agent org")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: Confirm you want to assign agent to all properties - Valuation Office Agency - GOV.UK")
  }

  "assignAgentToAll" should "return 200 Ok a valid form is submitted" in {
    when(mockAgentRelationshipService.assignAgent(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.assignAgentToAll(agentCode, "Some agent org")(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.ALL_PROPERTIES}"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Agent assigned to all properties - Valuation Office Agency - GOV.UK")
  }

  "unassignAgentFromAll" should "return 400 Bad Request when invalid form submitted" in {

    val res = testController.unassignAgentFromAll(agentCode, "Some agent org")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch("Confirm you want to unassign agent from all properties - Valuation Office Agency - GOV.UK")

  }

  "unassignAgentFromAll" should "return 200 Ok a valid form is submitted" in {
    when(mockAgentRelationshipService.unassignAgent(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.unassignAgentFromAll(agentCode, "Some agent org")(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.ALL_PROPERTIES}"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Agent has been unassigned from all your properties - Valuation Office Agency - GOV.UK")

  }

  "showRemoveAgentFromIpOrganisation" should "return 200 Ok" in {
    val res = testController.showRemoveAgentFromIpOrganisation(agentCode, "Some agent org")(FakeRequest())

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.removeFromAccount.title")} - Valuation Office Agency - GOV.UK")

  }

  "removeAgentFromIpOrganisation" should "return 400 Bad Request when invalid form submitted" in {

    val res = testController.removeAgentFromIpOrganisation(agentCode, "Some agent org", "some-back-link")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch("Error: Remove agent from your account - Valuation Office Agency - GOV.UK")
  }

  "removeAgentFromIpOrganisation" should "return 200 Ok a valid form is submitted" in {
    when(mockAgentRelationshipService.removeAgentFromOrganisation(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController.removeAgentFromIpOrganisation(agentCode, "Some agent org", "back-link")(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.RELATIONSHIP}"))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.manageAgent.removeFromAccount.confirmation.title")} - Valuation Office Agency - GOV.UK")
  }

}
