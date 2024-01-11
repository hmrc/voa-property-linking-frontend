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

package controllers.agent

import binders.propertylinks.GetPropertyLinksParameters
import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.AllMocks
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.HtmlPage

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class ManageAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  lazy val testController = new ManageAgentController(
    errorHandler = mockCustomErrorHandler,
    authenticated = preAuthenticatedActionBuilders(userIsAgent = false),
    agentRelationshipService = mockAgentRelationshipService,
    manageAgentView = manageAgentView,
    manageAgentViewOld = manageAgentViewOld,
    myAgentsView = myAgentsView,
    myAgentsViewOld = myAgentViewOld,
    featureSwitch = mockFeatureSwitch,
    removeAgentFromOrganisationView = removeAgentFromOrganisationView,
    unassignAgentFromPropertyView = unassignAgentFromPropertyView,
    addAgentToAllPropertiesView = addAgentToAllPropertyView,
    confirmAddAgentToAllPropertiesView = confirmAddAgentToAllPropertyView,
    unassignAgentFromAllPropertiesView = unassignAgentFromAllPropertiesView,
    confirmUnassignAgentFromAllPropertiesView = confirmUnassignAgentFromAllPropertiesView,
    confirmRemoveAgentFromOrganisationView = confirmRemoveAgentFromOrganisationView,
    manageAgentPropertiesView = manageAgentPropertiesView,
    manageAgentPropertiesViewOld = manageAgentPropertiesViewOld,
    manageAgentSessionRepo = mockSessionRepository
  )

  "showAgents" should "show the manage agent page" in {
    val propertyLinksCount = 1
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(
        organisationsAgentsListWithOneAgent.copy(agents = List(agentSummary.copy(propertyCount = 0)))))
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
    val agent = agentSummary.copy(propertyCount = 0)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))

    val res = testController.showManageAgent()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Are you sure you want to remove Some Agent Org from your account? - Valuation Office Agency - GOV.UK")
  }

  "manageAgent" should "show the manage agent page - in welsh" in {
    val agent = agentSummary.copy(propertyCount = 0)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))

    val res = testController.showManageAgent()(welshFakeRequest)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Ydych chi’n siŵr eich bod am dynnu ${agentSummary.name} o’ch cyfrif? - Valuation Office Agency - GOV.UK")
  }

  "manageAgentProperties" should "return the correct manage agent page with property links" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithTwoAgents))

    val res =
      testController.manageAgentProperties(agentCode, GetPropertyLinksParameters(), None, None, None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"${messages("propertyRepresentation.agentProperties.assigned.title")} - Valuation Office Agency - GOV.UK")

    html.html
      .getElementById("back-link")
      .attr("href") shouldBe controllers.agent.routes.ManageAgentController.showAgents.url

  }

  "manageAgentProperties" should "return the correct back link for provided parameters - propertyLinkId, valuationId, propertyLinkSubmissionId" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithTwoAgents))

    val res = testController
      .manageAgentProperties(agentCode, GetPropertyLinksParameters(), Some(1L), Some(1L), Some("subId"))(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.html
      .getElementById("back-link")
      .attr("href") shouldBe "http://localhost:9537/business-rates-valuation/property-link/1/valuations/1?submissionId=subId#agents-tab"

  }

  "manageAgentProperties" should "return the correct back link for provided parameters - valuationId, propertyLinkSubmissionId" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithTwoAgents))

    val res = testController
      .manageAgentProperties(agentCode, GetPropertyLinksParameters(), None, Some(1L), Some("subId"))(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.html
      .getElementById("back-link")
      .attr("href") shouldBe controllers.detailedvaluationrequest.routes.DvrController
      .myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "subId",
        valuationId = 1L,
        tabName = Some("agents-tab"))
      .url

  }

  "manageAgentProperties" should "return the correct back link for when organisation only  has one agent and no other parameter is provided" in {
    val agentCode = 1
    when(mockAgentRelationshipService.getMyAgentPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))
    when(mockAgentRelationshipService.getAgentNameAndAddress(any())(any()))
      .thenReturn(Future.successful(Some(agentDetails)))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent))

    val res = testController
      .manageAgentProperties(agentCode)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.html
      .getElementById("back-link")
      .attr("href") shouldBe "http://localhost:9542/business-rates-dashboard/home"

  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent but no property links" in {
    val agent = agentSummary.copy(propertyCount = 0)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))

    val res = testController.getManageAgentView()(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Are you sure you want to remove Some Agent Org from your account? - Valuation Office Agency - GOV.UK")

  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent but no property links - in welsh" in {
    val agent = agentSummary.copy(propertyCount = 0)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithNoAuthorisations))

    val res = testController.getManageAgentView()(welshFakeRequest).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Ydych chi’n siŵr eich bod am dynnu ${agentSummary.name} o’ch cyfrif? - Valuation Office Agency - GOV.UK")

  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and one property link (agent not assigned)" in {
    val agent = agentSummary.copy(propertyCount = 0)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView()(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and one property link (agent assigned)" in {
    val agent = agentSummary.copy(propertyCount = 1)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView()(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    val expectedQuestion = s"Are you sure you want to unassign ${agentSummary.name} from your property?"
    html.titleShouldMatch(s"$expectedQuestion - Valuation Office Agency - GOV.UK")
    html.html.getElementById("unassignFromProperty-question").text() shouldBe expectedQuestion
    html.html
      .getElementById("unassignFromProperty-p1")
      .text() shouldBe "For your property, the agent will not be able to:"
    verifyUnassignedPrivilegesDisplayed(html.html)
    html.html
      .getElementById("cancel-link")
      .attr("href") shouldBe s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=${agentSummary.representativeCode}"
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and more than one property links (agent not assigned)" in {
    val agent = agentSummary.copy(propertyCount = 0)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.getManageAgentView()(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
    html.html
      .getElementById("back-link")
      .attr("href") shouldBe s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=${agentSummary.representativeCode}"
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and more than one property links (agent assigned to some)" in {
    val agent = agentSummary.copy(propertyCount = 1)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentView()(FakeRequest()).futureValue.get

    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return the correct manage agent page when org has one agent and more than one property links (agent assigned to all)" in {
    val agent = agentSummary.copy(propertyCount = 2)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentView()(FakeRequest()).futureValue.get
    val html = HtmlPage(res)
    html.titleShouldMatch(s"${messages("propertyRepresentation.manageAgent.title")} - Valuation Office Agency - GOV.UK")
  }

  "getManageAgentView" should "return None for an invalid propertyLinks and agent details combination" in {
    val agent = agentSummary.copy(propertyCount = 9)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthorisation))

    val res = testController.getManageAgentView()(FakeRequest())
    res.futureValue shouldBe None
  }

  "submitManageAgent" should "return 400 Bad Request when an invalid manage property option is submitted " in {
    val agent = agentSummary.copy(propertyCount = 1, representativeCode = agentCode)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest().withFormUrlEncodedBody("manageAgentOption" -> "BLAH", "agentCode" -> s"$agentCode"))
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Are you sure you want to unassign ${agentSummary.name} from your property? - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 400 Bad Request when agentCode is not submitted " in {
    val agent = agentSummary.copy(propertyCount = 1, representativeCode = agentCode)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest().withFormUrlEncodedBody("manageAgentOption" -> "unassignFromAllProperties"))
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Are you sure you want to unassign ${agentSummary.name} from your property? - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 303 SEE OTHER when IP chooses to appoint agent to all properties" in {
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsListWithOneAgent.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${AssignToAllProperties.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/manage-agent/assign/to-all-properties")

  }

  "showAssignToAll" should "return 200 OK" in new AssignToAllTestCase with English {
    status(result) shouldBe OK
  }

  "showAssignToAll" should "display static content correctly in English" in new AssignToAllTestCase with English {
    explainerList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "see detailed property information",
      "see Check and Challenge case correspondence such as messages and emails",
      "send Check and Challenge cases"
    )
    cancelLink.text shouldBe "Cancel"
    cancelLink.attr("href") shouldBe routes.ManageAgentController
      .manageAgentProperties(agentToAppoint.representativeCode)
      .url
    confirmButton shouldBe "Confirm and assign"
  }

  "showAssignToAll" should "display static content correctly in Welsh" in new AssignToAllTestCase with Welsh {
    explainerList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "gweld gwybodaeth eiddo fanwl",
      "gweld gohebiaeth achosion Gwirio a Herio megis negeseuon ac e-byst",
      "anfon achosion Gwirio a Herio"
    )
    cancelLink.text shouldBe "Canslo"
    cancelLink.attr("href") shouldBe routes.ManageAgentController
      .manageAgentProperties(agentToAppoint.representativeCode)
      .url
    confirmButton shouldBe "Cadarnhau a neilltuo"
  }

  "showAssignToAll" should "display dynamic content correctly in English when assigning to one property" in new AssignToAllTestCase
  with English {
    override lazy val numberOfIpPropertyLinks = 1
    doc.title shouldBe s"Are you sure you want to assign ${agentToAppoint.name} to your property? - Valuation Office Agency - GOV.UK"
    heading shouldBe s"Are you sure you want to assign ${agentToAppoint.name} to your property?"
    explainerIntro shouldBe "For your property, the agent will be able to:"
  }

  "showAssignToAll" should "display dynamic content correctly in Welsh when assigning to one property" in new AssignToAllTestCase
  with Welsh {
    override lazy val numberOfIpPropertyLinks = 1
    doc.title shouldBe s"Ydych chi’n siŵr eich bod am neilltuo ${agentToAppoint.name} i’ch eiddo? - Valuation Office Agency - GOV.UK"
    heading shouldBe s"Ydych chi’n siŵr eich bod am neilltuo ${agentToAppoint.name} i’ch eiddo?"
    explainerIntro shouldBe "Ar gyfer eich eiddo, bydd yr asiant yn gallu:"
  }

  "showAssignToAll" should "display dynamic content correctly in English when assigning to multiple properties" in new AssignToAllTestCase
  with English {
    override lazy val numberOfIpPropertyLinks = 2
    doc.title shouldBe s"Are you sure you want to assign ${agentToAppoint.name} to all your properties? - Valuation Office Agency - GOV.UK"
    heading shouldBe s"Are you sure you want to assign ${agentToAppoint.name} to all your properties?"
    explainerIntro shouldBe "For all your properties, the agent will be able to:"
  }

  "showAssignToAll" should "display dynamic content correctly in Welsh when assigning to multiple properties" in new AssignToAllTestCase
  with Welsh {
    override lazy val numberOfIpPropertyLinks = 2
    doc.title shouldBe s"Ydych chi’n siŵr eich bod am neilltuo ${agentToAppoint.name} i’ch holl eiddo? - Valuation Office Agency - GOV.UK"
    heading shouldBe s"Ydych chi’n siŵr eich bod am neilltuo ${agentToAppoint.name} i’ch holl eiddo?"
    explainerIntro shouldBe "Ar gyfer eich holl eiddo, bydd yr asiant yn gallu:"
  }

  "submitManageAgent" should "return 303 Redirect when agent is appointed to some properties & from manage agent journey should be true" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsListWithOneAgent.copy(
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
          "agentCode=12345&agentAppointed=BOTH&backLinkUrl=%2F" +
          "business-rates-property-linking%2Fmy-organisation%2Fmanage-agent&fromManageAgentJourney=true")
  }

  "submitManageAgent" should "return 303 SEE OTHER when IP chooses to unassigned agent from all properties" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsListWithOneAgent.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(FakeRequest()
      .withFormUrlEncodedBody("manageAgentOption" -> s"${UnassignFromAllProperties.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe SEE_OTHER

    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties")
  }

  "submitManageAgent" should "return 303 Redirect when IP chooses to unassign agent from some properties" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsListWithOneAgent.copy(
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
      Future.successful(organisationsAgentsListWithOneAgent.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${RemoveFromYourAccount.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation")
  }

  "showRemoveAgentFromIpOrganisation" should "return 200 Ok when IP removes agent from account" in {
    val agent = agentSummary.copy(propertyCount = 1, representativeCode = agentCode)
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent.copy(agents = List(agent))))
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agent)))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))

    val res = testController.showRemoveAgentFromIpOrganisation()(FakeRequest())

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Are you sure you want to remove Some Agent Org from your account? - Valuation Office Agency - GOV.UK")
  }

  "submitManageAgent" should "return 200 Ok when IP chooses to appoint agent to only property" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(
      Future.successful(organisationsAgentsListWithOneAgent.copy(
        agents = List(agentSummary.copy(propertyCount = 1, representativeCode = agentCode)))))
    when(mockAgentRelationshipService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultWithTwoAuthsAgentAssignedToOne))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.submitManageAgent(agentCode)(
      FakeRequest()
        .withFormUrlEncodedBody("manageAgentOption" -> s"${AssignToYourProperty.name}", "agentName" -> "Agent Org"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/manage-agent/assign/to-all-properties")
  }

  "assignAgentToAll" should "return 400 Bad Request when invalid form submitted" in {
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.assignAgentToAll(agentCode, "Some agent org")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.title shouldBe s"Error: Are you sure you want to assign Some agent org to all your properties? - Valuation Office Agency - GOV.UK"
  }

  "assignAgentToAll" should "return 303 SEE OTHER when valid form is submitted" in {
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any())).thenReturn(Future.successful(AgentList(
      1,
      List(
        AgentSummary(
          organisationId = 1,
          representativeCode = 1,
          name = "name",
          appointedDate = LocalDate.now,
          propertyCount = 1,
          listYears = Some(Seq("2017", "2023"))))
    )))
    when(mockAgentRelationshipService.postAgentAppointmentChange(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.assignAgentToAll(agentCode, "Some agent org")(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.ALL_PROPERTIES}"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/manage-agent/assign/to-all-properties/confirm")
  }

  "confirmAssignAgentToAll" should "return 200 OK" in new ConfirmAssignToAllTestCase with English {
    status(result) shouldBe OK
  }

  "confirmAssignAgentToAll" should "display static content correctly in English" in new ConfirmAssignToAllTestCase
  with English {
    nextStepsSubhead shouldBe "What happens next"
    accountHomeLink.text shouldBe "Go to your account home"
    accountHomeLink.attr("href") shouldBe "http://localhost:9542/business-rates-dashboard/home"
  }

  "confirmAssignAgentToAll" should "display static content correctly in Welsh" in new ConfirmAssignToAllTestCase
  with Welsh {
    nextStepsSubhead shouldBe "Yr hyn sy’n digwydd nesaf"
    accountHomeLink.text shouldBe "Ewch i hafan eich cyfrif"
    accountHomeLink.attr("href") shouldBe "http://localhost:9542/business-rates-dashboard/home"
  }

  "confirmAssignAgentToAll" should "display dynamic content correctly in English when assigned to one property" in new ConfirmAssignToAllTestCase
  with English {
    override lazy val numberOfIpPropertyLinks: Int = 1
    doc.title shouldBe s"${appointedAgent.name} has been assigned to your property - Valuation Office Agency - GOV.UK"
    panel shouldBe s"${appointedAgent.name} has been assigned to your property"
    explainer shouldBe "The agent can act for you on your property."
    nextStepsContent shouldBe "You can unassign this agent from your property at any time."
  }

  "confirmAssignAgentToAll" should "display dynamic content correctly in Welsh when assigned to one property" in new ConfirmAssignToAllTestCase
  with Welsh {
    override lazy val numberOfIpPropertyLinks: Int = 1
    doc.title shouldBe s"Mae ${appointedAgent.name} wedi’i neilltuo i’ch eiddo - Valuation Office Agency - GOV.UK"
    panel shouldBe s"Mae ${appointedAgent.name} wedi’i neilltuo i’ch eiddo"
    explainer shouldBe "Gall yr asiant weithredu ar eich rhan ar o’ch eiddo."
    nextStepsContent shouldBe "Gallwch ddadneilltuo’r asiant hwn o’ch eiddo ar unrhyw adeg."
  }

  "confirmAssignAgentToAll" should "display dynamic content correctly in English when assigned to multiple properties" in new ConfirmAssignToAllTestCase
  with English {
    override lazy val numberOfIpPropertyLinks: Int = 2
    doc.title shouldBe s"${appointedAgent.name} has been assigned to all your properties - Valuation Office Agency - GOV.UK"
    panel shouldBe s"${appointedAgent.name} has been assigned to all your properties"
    explainer shouldBe "The agent can act for you on all of your properties."
    nextStepsContent shouldBe "You can unassign this agent from your properties at any time."
  }

  "confirmAssignAgentToAll" should "display dynamic content correctly in Welsh when assigned to multiple properties" in new ConfirmAssignToAllTestCase
  with Welsh {
    override lazy val numberOfIpPropertyLinks: Int = 2
    doc.title shouldBe s"Mae ${appointedAgent.name} wedi’i neilltuo i’ch holl eiddo - Valuation Office Agency - GOV.UK"
    panel shouldBe s"Mae ${appointedAgent.name} wedi’i neilltuo i’ch holl eiddo"
    explainer shouldBe "Gall yr asiant weithredu ar eich rhan ar gyfer pob un o’ch eiddo."
    nextStepsContent shouldBe "Gallwch ddadneilltuo’r asiant hwn o’ch eiddo ar unrhyw adeg."
  }

  "unassignAgentFromAll" should "return 400 Bad Request when invalid form submitted" in {

    val res = testController.unassignAgentFromAll(agentCode, "Some agent org")(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Are you sure you want to unassign Some agent org from all your properties? - Valuation Office Agency - GOV.UK")

  }
  "unassignAgentFromAll" should "return 400 Bad Request when invalid form submitted - in welsh" in {

    val agentName = "Some agent org"
    val res = testController.unassignAgentFromAll(agentCode, agentName)(
      welshFakeRequest.withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Ydych chi’n siŵr eich bod am ddadneilltuo $agentName o’ch holl eiddo? - Valuation Office Agency - GOV.UK")

  }

  "unassignAgentFromAll" should "return 303 Ok a valid form is submitted" in {
    when(mockAgentRelationshipService.postAgentAppointmentChange(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any())).thenReturn(Future.successful(10))

    val res = testController.unassignAgentFromAll(agentCode, "Some agent org")(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.ALL_PROPERTIES}"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe
      Some("/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties/confirmation")

  }

  "showRemoveAgentFromIpOrganisation" should "return 200 Ok" in {
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agentSummary)))
    val res = testController.showRemoveAgentFromIpOrganisation()(FakeRequest())

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Are you sure you want to remove Some Agent Org from your account? - Valuation Office Agency - GOV.UK")
    html.html
      .getElementById("remove-agent-from-org-p1")
      .text() shouldBe "They will no longer be able to add properties to your account and act on them for you."
    html.html
      .getElementById("remove-agent-from-org-p2")
      .text() shouldBe "You will no longer be able to assign properties to them or have them act for you."
  }

  "showRemoveAgentFromIpOrganisation" should "return 200 Ok - in welsh" in {
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agentSummary)))
    val res = testController.showRemoveAgentFromIpOrganisation()(welshFakeRequest)

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Ydych chi’n siŵr eich bod am dynnu ${agentSummary.name} o’ch cyfrif? - Valuation Office Agency - GOV.UK")
    html.html
      .getElementById("remove-agent-from-org-p1")
      .text() shouldBe "Ni fyddant bellach yn gallu ychwanegu eiddo at eich cyfrif a gweithredu arnynt ar eich rhan."
    html.html
      .getElementById("remove-agent-from-org-p2")
      .text() shouldBe "Ni fyddwch bellach yn gallu aseinio eiddo iddynt na’u cael i weithredu ar eich rhan."
  }

  "removeAgentFromIpOrganisation" should "return 400 Bad Request when invalid form submitted" in {

    val res = testController
      .removeAgentFromIpOrganisation(agentCode, "Some agent org", RedirectUrl("http://localhost/some-back-link"))(
        FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: Are you sure you want to remove Some agent org from your account? - Valuation Office Agency - GOV.UK")
  }

  "removeAgentFromIpOrganisation" should "return 400 Bad Request when invalid form submitted - in welsh" in {

    val agentName = "Some agent org"
    val res = testController
      .removeAgentFromIpOrganisation(agentCode, agentName, RedirectUrl("http://localhost/some-back-link"))(
        welshFakeRequest.withFormUrlEncodedBody("agentCode" -> s"$agentCode"))

    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      s"Gwall: Ydych chi’n siŵr eich bod am dynnu $agentName o’ch cyfrif? - Valuation Office Agency - GOV.UK")
  }

  "removeAgentFromIpOrganisation" should "return 200 Ok a valid form is submitted" in {
    when(mockAgentRelationshipService.postAgentAppointmentChange(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController
      .removeAgentFromIpOrganisation(agentCode, "Some agent org", RedirectUrl("http://localhost/some-back-link"))(
        FakeRequest()
          .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "scope" -> s"${AppointmentScope.RELATIONSHIP}"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(
      "/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation/confirm")
  }

  "confirmRemoveAgentFromOrganisation" should "return 200 Ok and display page" in {
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agentSummary)))
    when(mockAgentRelationshipService.postAgentAppointmentChange(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController.confirmRemoveAgentFromOrganisation()(FakeRequest())

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch("Some Agent Org has been removed from your account - Valuation Office Agency - GOV.UK")
    html.html.getElementById("remove-agent-confirmation-p1").text() shouldBe "The agent can no longer act for you."
    html.html
      .getElementById("remove-agent-confirmation-p2")
      .text() shouldBe s"If you want the agent to act for you again, you can reappoint them to your account using agent code ${agentSummary.representativeCode.toString()}."
  }

  "confirmRemoveAgentFromOrganisation" should "return 200 Ok and display page  - in welsh" in {
    when(mockSessionRepository.get[AgentSummary](any(), any())).thenReturn(Future.successful(Some(agentSummary)))
    when(mockAgentRelationshipService.postAgentAppointmentChange(any())(any()))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("some-id")))

    val res = testController.confirmRemoveAgentFromOrganisation()(welshFakeRequest)

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(s"Mae ${agentSummary.name} wedi’i dynnu o’ch cyfrif - Valuation Office Agency - GOV.UK")
    html.html
      .getElementById("remove-agent-confirmation-p1")
      .text() shouldBe "Ni all yr asiant weithredu ar eich rhan mwyach."
    html.html
      .getElementById("remove-agent-confirmation-p2")
      .text() shouldBe s"Os ydych am i’r asiant weithredu ar eich rhan eto, gallwch ei ailbenodi i’ch cyfrif gan ddefnyddio cod asiant ${agentSummary.representativeCode.toString()}."
  }

  type English = EnglishRequest
  type Welsh = WelshRequest

  trait AssignToAllTestCase { self: RequestLang =>
    lazy val numberOfIpPropertyLinks: Int = 2
    val agentToAppoint: AgentSummary = agentSummary

    when(mockSessionRepository.get[AgentSummary](any, any)).thenReturn(Future.successful(Some(agentToAppoint)))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any))
      .thenReturn(Future.successful(numberOfIpPropertyLinks))

    val result: Future[Result] = testController.showAssignToAll(self.fakeRequest)
    val doc: Document = Jsoup.parse(contentAsString(result))
    val heading: String = doc.getElementsByTag("h1").text
    val explainerIntro: String = doc.getElementById("explainer-intro").text
    val explainerList: Element = doc.getElementById("explainer-list")
    val cancelLink: Element = doc.getElementById("cancel-link")
    val confirmButton: String = doc.getElementById("confirm-button").text
  }

  trait ConfirmAssignToAllTestCase { self: RequestLang =>
    lazy val numberOfIpPropertyLinks: Int = 2
    val appointedAgent: AgentSummary = agentSummary

    when(mockSessionRepository.get[AgentSummary](any, any)).thenReturn(Future.successful(Some(appointedAgent)))
    when(mockAgentRelationshipService.getMyOrganisationPropertyLinksCount()(any))
      .thenReturn(Future.successful(numberOfIpPropertyLinks))

    val result: Future[Result] = testController.confirmAssignAgentToAll(self.fakeRequest)
    val doc: Document = Jsoup.parse(contentAsString(result))
    val panel: String = doc.getElementsByTag("h1").text
    val explainer: String = doc.getElementById("explainer").text
    val nextStepsSubhead: String = doc.getElementById("next-steps-subhead").text
    val nextStepsContent: String = doc.getElementById("next-steps-content").text
    val accountHomeLink: Element = doc.getElementById("account-home-link")
  }

}
