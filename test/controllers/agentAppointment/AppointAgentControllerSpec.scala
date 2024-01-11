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

import binders.pagination.PaginationParameters
import binders.propertylinks.ExternalPropertyLinkManagementSortField.{ADDRESS, AGENT}
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder}
import controllers.VoaPropertyLinkingSpec
import models.{AgentAppointBulkAction, AgentRevokeBulkAction, SessionPropertyLinks}
import models.propertyrepresentation._
import models.searchApi._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import services.{AgentRelationshipService, AppointRevokeException}
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.{Formatters, HtmlPage, StubGroupAccountConnector}

import scala.collection.mutable
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class AppointAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks with OptionValues {

  val agent = groupAccount(true).copy(agentCode = Some(agentCode))
  val backLinkUrl = RedirectUrl("http://localhost/some-back-link")
  val testAgents = Seq(OwnerAuthAgent(1L, agent.id, "organisationName", 1L))

  "getMyOrganisationPropertyLinksWithAgentFiltering" should "show the appoint agent properties page" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

    when(
      mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(
        any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockAppointRevokeService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[FilterAppointProperties](any())(any(), any()))
      .thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res = testController
      .getMyOrganisationPropertyLinksWithAgentFiltering(PaginationParameters(), agentCode, None, backLinkUrl, false)(
        FakeRequest())
    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
  }

  "getMyOrganisationPropertyLinksWithAgentFiltering" should "display correctly in English" in new UnfilteredResultsTestCase
  with English {
    doc.title shouldBe s"Choose which of your properties you want to assign ${agent.companyName} to - Valuation Office Agency - GOV.UK"
    heading shouldBe s"Choose which of your properties you want to assign ${agent.companyName} to"
    explainerIntro shouldBe "For the properties you select, the agent will be able to:"
    explainerList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "see detailed property information",
      "see Check and Challenge case correspondence such as messages and emails",
      "send Check and Challenge cases"
    )

    searchLegend shouldBe "Search your properties"
    addressInputLabel shouldBe "Address"
    agentSelectLabel.value shouldBe "Agent"

    private val expectedAgents = getMyOrganisationAgentsResponse.agents.filterNot(_.organisationId == agent.id)

    agentSelect.value.children.first.text shouldBe s"Choose from ${expectedAgents.size} agents"
    selectableAgents.value should contain theSameElementsInOrderAs expectedAgents.map(_.name)
    searchButton shouldBe "Search"
    clearSearch.text shouldBe "Clear search"
    clearSearch.attr("href") shouldBe routes.AppointAgentController
      .getMyOrganisationPropertyLinksWithAgentFiltering(
        PaginationParameters(),
        agentCode = agentCode,
        agentAppointed = initialAgentAppointedQueryParam,
        backLinkUrl = backLinkQueryParam,
        fromManageAgentJourney = false
      )
      .url

    selectAll shouldBe "Select all"
    filterNoAgent.text shouldBe "Only show properties with no agent"
    filterNoAgent.attr("href") shouldBe routes.AppointAgentController
      .getMyOrganisationPropertyLinksWithAgentFiltering(
        PaginationParameters(),
        agentCode = agentCode,
        agentAppointed = Some(AgentPropertiesFilter.No.name),
        backLinkUrl = backLinkQueryParam,
        fromManageAgentJourney = false
      )
      .url

    sortByAddress.text shouldBe "Address"
    sortByAddress.attr("href") shouldBe controllers.agentAppointment.routes.AppointAgentController
      .sortPropertiesForAppoint(
        sortField = ADDRESS,
        pagination = initialPaginationParams,
        agentCode = agentCode,
        agentAppointed = initialAgentAppointedQueryParam,
        backLinkUrl = backLinkQueryParam,
        fromManageAgentJourney = false
      )
      .url
    sortByAppointedAgents.text shouldBe "Appointed agents"
    sortByAppointedAgents.attr("href") shouldBe controllers.agentAppointment.routes.AppointAgentController
      .sortPropertiesForAppoint(
        sortField = AGENT,
        pagination = initialPaginationParams,
        agentCode = agentCode,
        agentAppointed = initialAgentAppointedQueryParam,
        backLinkUrl = backLinkQueryParam,
        fromManageAgentJourney = false
      )
      .url
    confirmButton shouldBe "Continue"
  }

  it should "display correctly in Welsh" in new UnfilteredResultsTestCase with Welsh {
    doc.title shouldBe s"Dewis pa eiddo yr hoffech ei neilltuo i ${agent.companyName} - Valuation Office Agency - GOV.UK"
    heading shouldBe s"Dewis pa eiddo yr hoffech ei neilltuo i ${agent.companyName}"
    explainerIntro shouldBe "Ar gyfer yr eiddo a ddewiswch, bydd yr asiant yn gallu:"
    explainerList.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "gweld gwybodaeth eiddo fanwl",
      "gweld gohebiaeth achosion Gwirio a Herio megis negeseuon ac e-byst",
      "anfon achosion Gwirio a Herio"
    )

    searchLegend shouldBe "Chwiliwch eich eiddo"
    addressInputLabel shouldBe "Cyfeiriad"
    agentSelectLabel.value shouldBe "Asiant"

    private val expectedAgents = getMyOrganisationAgentsResponse.agents.filterNot(_.organisationId == agent.id)

    agentSelect.value.children.first.text shouldBe s"Dewiswch o ${expectedAgents.size} o asiantiaid"
    selectableAgents.value should contain theSameElementsInOrderAs expectedAgents.map(_.name)
    searchButton shouldBe "Chwilio"
    clearSearch.text shouldBe "Clirio’r chwiliad"
    clearSearch.attr("href") shouldBe routes.AppointAgentController
      .getMyOrganisationPropertyLinksWithAgentFiltering(
        PaginationParameters(),
        agentCode,
        initialAgentAppointedQueryParam,
        backLinkQueryParam,
        false
      )
      .url

    selectAll shouldBe "Dewiswch popeth"
    filterNoAgent.text shouldBe "Yn dangos eiddo heb unrhyw asiant yn unig"
    filterNoAgent.attr("href") shouldBe routes.AppointAgentController
      .getMyOrganisationPropertyLinksWithAgentFiltering(
        PaginationParameters(),
        agentCode,
        Some(AgentPropertiesFilter.No.name),
        backLinkQueryParam,
        false
      )
      .url

    sortByAddress.text shouldBe "Cyfeiriad"
    sortByAddress.attr("href") shouldBe controllers.agentAppointment.routes.AppointAgentController
      .sortPropertiesForAppoint(
        sortField = ADDRESS,
        pagination = initialPaginationParams,
        agentCode = agentCode,
        agentAppointed = initialAgentAppointedQueryParam,
        backLinkUrl = backLinkQueryParam,
        fromManageAgentJourney = false
      )
      .url
    sortByAppointedAgents.text shouldBe "Asiantiaid penodedig"
    sortByAppointedAgents.attr("href") shouldBe controllers.agentAppointment.routes.AppointAgentController
      .sortPropertiesForAppoint(
        sortField = AGENT,
        pagination = initialPaginationParams,
        agentCode = agentCode,
        agentAppointed = initialAgentAppointedQueryParam,
        backLinkUrl = backLinkQueryParam,
        fromManageAgentJourney = false
      )
      .url
    confirmButton shouldBe "Yn eich blaen"
  }

  it should "return 200 OK" in new UnfilteredResultsTestCase with English {
    status(result) shouldBe OK
  }

  it should "display the correct agent select placeholder option when only one selectable agent in English" in new UnfilteredResultsTestCase
  with English {
    // one agent is currently being appointed, so is filtered out
    override lazy val getMyOrganisationAgentsResponse: AgentList = organisationsAgentsListWithTwoAgents
    agentSelect.value.children.first.text shouldBe "Choose from 1 agent"
    selectableAgents.value should contain theSameElementsAs organisationsAgentsListWithTwoAgents.agents.collect {
      case appointedAgent if appointedAgent.organisationId != agent.id => appointedAgent.name
    }
  }

  it should "display the correct agent select placeholder option when only one selectable agent in Welsh" in new UnfilteredResultsTestCase
  with Welsh {
    // one agent is currently being appointed, so is filtered out
    override lazy val getMyOrganisationAgentsResponse: AgentList = organisationsAgentsListWithTwoAgents
    agentSelect.value.children.first.text shouldBe "Dewis o 1 asiant"
    selectableAgents.value should contain theSameElementsAs organisationsAgentsListWithTwoAgents.agents.collect {
      case appointedAgent if appointedAgent.organisationId != agent.id => appointedAgent.name
    }
  }

  it should "hide the agent dropdown when assigning properties to a sole already-appointed agent" in new UnfilteredResultsTestCase
  with English {
    override lazy val getMyOrganisationAgentsResponse: AgentList = organisationsAgentsListWithOneAgent

    agentSelectLabel should not be defined
    agentSelect should not be defined
  }

  it should "display returned property links with assigned agents in English" in new UnfilteredResultsTestCase
  with English {
    resultsAddresses should contain theSameElementsInOrderAs ownerAuthResult.authorisations.map { authorisation =>
      s"Appoint ${Formatters.capitalisedAddress(authorisation.address)}"
    }
    resultsAgents should contain theSameElementsInOrderAs ownerAuthResult.authorisations.map {
      _.agents.collect {
        case appointedAgent if appointedAgent.organisationId != agent.id => appointedAgent.organisationName
      }
    }
  }

  it should "display returned property links with assigned agents in Welsh" in new UnfilteredResultsTestCase
  with Welsh {
    resultsAddresses should contain theSameElementsInOrderAs ownerAuthResult.authorisations.map { authorisation =>
      s"Penodi ${Formatters.capitalisedAddress(authorisation.address)}"
    }
    resultsAgents should contain theSameElementsInOrderAs ownerAuthResult.authorisations.map {
      _.agents.collect {
        case appointedAgent if appointedAgent.organisationId != agent.id => appointedAgent.organisationName
      }
    }
  }

  "paginatePropertiesForAppoint" should "show the requested appoint agent properties page" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

    when(
      mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(
        any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[FilterAppointProperties](any())(any(), any()))
      .thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res = testController
      .paginatePropertiesForAppoint(PaginationParameters().nextPage, agentCode, None, backLinkUrl, false)(FakeRequest())
    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
  }

  "sortPropertiesForAppoint" should "show a sorted appoint agent properties page" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address 1", "localAuthorityRef", testAgents)
    val testOwnerAuth2 =
      OwnerAuthorisation(2L, "APPROVED", "1111112", 1L, "address 2", "localAuthorityRef2", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(
        start = 1,
        size = 15,
        filterTotal = 2,
        total = 2,
        authorisations = Seq(testOwnerAuth, testOwnerAuth2))

    when(
      mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(
        any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession(
        filters = FilterAppointProperties(None, None, ExternalPropertyLinkManagementSortOrder.ASC)))))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[FilterAppointProperties](any())(any(), any()))
      .thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res = testController.sortPropertiesForAppoint(
      sortField = ExternalPropertyLinkManagementSortField.ADDRESS,
      pagination = PaginationParameters(),
      agentCode = agentCode,
      agentAppointed = None,
      backLinkUrl = backLinkUrl,
      fromManageAgentJourney = false
    )(FakeRequest())
    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContain("#sort-by-address-head.sort_desc", 1)
  }

  "showFilterPropertiesForAppoint" should "display the filterPropertiesForAppoint page" in new UnfilteredResultsTestCase
  with English {
    override lazy val result: Future[Result] = testController.showFilterPropertiesForAppoint(
      initialPaginationParams,
      agentCode,
      initialAgentAppointedQueryParam,
      backLinkQueryParam,
      false)(fakeRequest)

    heading shouldBe s"Choose which of your properties you want to assign ${agent.companyName} to"
  }

  "filterPropertiesForAppoint" should "show a filtered appoint agent properties page" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address 1", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 2, total = 2, authorisations = Seq(testOwnerAuth))

    when(
      mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(
        any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[FilterAppointProperties](any())(any(), any()))
      .thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res =
      testController.filterPropertiesForAppoint(PaginationParameters(), agentCode, None, backLinkUrl, false)(
        FakeRequest().withFormUrlEncodedBody(
          "address"     -> "address 1",
          "agentCode"   -> "12345",
          "backLinkUrl" -> backLinkUrl.unsafeValue
        ))

    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("ADDRESS 1")
  }

  it should "show an error when nothing is entered in English" in new FilterPropertiesToAppointEmptySearchTestCase
  with English {
    status(errorResult) shouldBe BAD_REQUEST
    emptySearchError.value shouldBe "You must enter something to search for"
    errorDoc.title shouldBe s"Error: Choose which of your properties you want to assign $ggExternalId to - Valuation Office Agency - GOV.UK"
  }

  it should "show an error when nothing is entered in Welsh" in new FilterPropertiesToAppointEmptySearchTestCase
  with Welsh {
    status(errorResult) shouldBe BAD_REQUEST
    emptySearchError.value shouldBe "Mae’n rhaid i chi nodi rhywbeth i chwilio amdano"
    errorDoc.title shouldBe s"Gwall: Dewis pa eiddo yr hoffech ei neilltuo i $ggExternalId - Valuation Office Agency - GOV.UK"
  }

  it should "remember the last searched-for agent in the dropdown" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address 1", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 2, total = 2, authorisations = Seq(testOwnerAuth))

    when(mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockAppointRevokeService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithTwoAgents))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[FilterAppointProperties](any())(any(), any()))
      .thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res =
      testController.filterPropertiesForAppoint(PaginationParameters(), agentCode, None, backLinkUrl, false)(
        FakeRequest().withFormUrlEncodedBody(
          "address"     -> "address 1",
          "agent"       -> "Some Agent Org",
          "backLinkUrl" -> backLinkUrl.unsafeValue
        ))

    status(res) shouldBe OK

    val page = Jsoup.parse(contentAsString(res))
    val dropdown = page.getElementById("agent-select")
    val firstOption = dropdown.child(0).text()

    firstOption shouldBe "Some Agent Org"
  }

  it should "submit appoint agent request and redirect to summary page" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[AppointAgentToSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.appointAgentSummary(agentCode, None, backLinkUrl)(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"           -> s"$agentCode",
        "name"                -> s"$companyName",
        "checkPermission"     -> "StartAndContinue",
        "challengePermission" -> "StartAndContinue",
        "linkIds[]"           -> "1",
        "backLinkUrl"         -> backLinkUrl.unsafeValue
      ))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/appoint/properties/confirm")

  }

  it should "return 200 OK on success" in new AppointToSomeConfirmationTestCase with English {
    status(result) shouldBe OK
  }

  it should "display content correctly in English" in new AppointToSomeConfirmationTestCase with English {
    doc.title shouldBe s"${agentAppointAction.name} has been assigned to your selected properties - Valuation Office Agency - GOV.UK"
    panel shouldBe s"${agentAppointAction.name} has been assigned to your selected properties"
    explainer shouldBe "The agent can act for you on any of the properties you selected."
    nextStepsSubhead shouldBe "What happens next"
    nextStepsContent shouldBe "You can unassign this agent from your properties at any time."
    accountHomeLink.text shouldBe "Go to your account home"
    accountHomeLink.attr("href") shouldBe applicationConfig.dashboardUrl("home")
  }

  it should "display content correctly in Welsh" in new AppointToSomeConfirmationTestCase with Welsh {
    doc.title shouldBe s"Mae ${agentAppointAction.name} wedi’i neilltuo i’r eiddo a ddewiswyd gennych - Valuation Office Agency - GOV.UK"
    panel shouldBe s"Mae ${agentAppointAction.name} wedi’i neilltuo i’r eiddo a ddewiswyd gennych"
    explainer shouldBe "Gall yr asiant weithredu ar eich rhan ar gyfer unrhyw un o’r eiddo a ddewiswyd gennych."
    nextStepsSubhead shouldBe "Beth sy’n digwydd nesaf"
    nextStepsContent shouldBe "Gallwch ddadneilltuo’r asiant hwn o’ch eiddo ar unrhyw adeg."
    accountHomeLink.text shouldBe "Ewch i hafan eich cyfrif"
    accountHomeLink.attr("href") shouldBe applicationConfig.dashboardUrl("home")
  }

  it should "show not found  page when no agent data is cached" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))

    val res = testController.confirmAppointAgentToSome()(FakeRequest())

    status(res) shouldBe NOT_FOUND

  }

  it should "show the appoint agent properties page when the summary has form errors" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))

    val res = testController.appointAgentSummary(agentCode, None, backLinkUrl)(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode", "backLinkUrl" -> "/some/back/link"))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
  }

  it should "show the appoint agent properties page when an appointment fails" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(AppointRevokeException("")))

    val res = testController.appointAgentSummary(agentCode, None, backLinkUrl)(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "linkIds[]" -> "1", "backLinkUrl" -> "/some/back/link"))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
    page.titleShouldMatch(
      s"Error: Choose which of your properties you want to assign $ggExternalId to - Valuation Office Agency - GOV.UK")
  }

  "showAppointAgentSummary" should "display the filterPropertiesForAppoint page" in new UnfilteredResultsTestCase
  with English {
    override lazy val result: Future[Result] =
      testController.showAppointAgentSummary(agentCode, initialAgentAppointedQueryParam, backLinkQueryParam, false)(
        fakeRequest)

    heading shouldBe s"Choose which of your properties you want to assign ${agent.companyName} to"
  }

  "selectAgentPropertiesSearchSort" should "show a list of properties available for removal from a agent" in {
    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.getMyAgentPropertyLinks(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))
    when(mockSessionRepo.saveOrUpdate[SessionPropertyLinks](any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res =
      testController.selectAgentPropertiesSearchSort(PaginationParameters(), agentCode)(
        FakeRequest().withFormUrlEncodedBody(
          "agentCode"      -> s"$agentCode",
          "agentCodeRadio" -> "1"
        ))

    status(res) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText(testOwnerAuth.address)

    page.html
      .getElementById("question-text")
      .text() shouldBe "Which of your properties do you want to unassign gg-ext-id from?"
    verifyUnassignedPrivilegesDisplayed(page.html)
  }

  "selectAgentPropertiesSearchSort" should "show a list of properties available for removal from a agent - in welsh" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.getMyAgentPropertyLinks(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))
    when(mockSessionRepo.saveOrUpdate[SessionPropertyLinks](any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res =
      testController.selectAgentPropertiesSearchSort(PaginationParameters(), agentCode)(
        welshFakeRequest.withFormUrlEncodedBody(
          "agentCode"      -> s"$agentCode",
          "agentCodeRadio" -> "1"
        ))

    status(res) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.html
      .getElementById("question-text")
      .text() shouldBe "O ba eiddo ydych chi am ddadneilltuo gg-ext-id?"
    verifyUnassignedPrivilegesDisplayed(page.html, isWelsh = true)

  }

  "paginateRevokeProperties" should "show the requested revoke properties page" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))
    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.getMyAgentPropertyLinks(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.paginateRevokeProperties(PaginationParameters().nextPage, 1L)(FakeRequest())

    status(res) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText(testOwnerAuth.address)
  }

  "sortRevokePropertiesByAddress" should "show a sorted revoke properties page" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))
    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address 1", "localAuthorityRef", testAgents)
    val testOwnerAuth2 =
      OwnerAuthorisation(2L, "APPROVED", "1122222", 2L, "address 2", "localAuthorityRef2", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(
        start = 1,
        size = 15,
        filterTotal = 2,
        total = 2,
        authorisations = Seq(testOwnerAuth, testOwnerAuth2))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.getMyAgentPropertyLinks(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.sortRevokePropertiesByAddress(PaginationParameters(), 1L)(FakeRequest())

    status(res) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContain("th.sort_desc", 1)
  }

  "filterPropertiesForRevoke" should "show a filtered revoke properties page" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address 1", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 2, total = 2, authorisations = Seq(testOwnerAuth))

    when(mockAppointRevokeService.getMyAgentPropertyLinks(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res = testController.filterPropertiesForRevoke(PaginationParameters(), agentCode)(
      FakeRequest().withFormUrlEncodedBody(
        "address" -> "address 1"
      ))

    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("ADDRESS 1")
  }

  "viewing select agent properties search sort page" should "show no properties when the agent has no properties appointed" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    val testOwnerAuthResult = OwnerAuthResult(start = 1, size = 0, filterTotal = 0, total = 0, authorisations = Seq())

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.getMyAgentPropertyLinks(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.selectAgentPropertiesSearchSort(PaginationParameters(), 1L)(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"      -> "1",
        "agentCodeRadio" -> "1"
      ))

    status(res) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("There are no properties to display")
  }

  "revoke agent summary page" should "redirect to the success page when all is well" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockRevokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))
    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))

    val res = testController.revokeAgentSummary(PaginationParameters(), agentCode)(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"        -> testAgentAccount.companyName,
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> backLinkUrl.unsafeValue
      ))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/revoke/properties/confirm")
  }

  "show revoke agent summary page" should "render a success screen when all is well" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    val session = RevokeAgentFromSomePropertiesSession(
      agentRevokeAction = Some(
        AgentRevokeBulkAction(
          agentCode = 123L,
          name = "some agent",
          propertyLinkIds = List(),
          backLinkUrl = backLinkUrl.unsafeValue)))

    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(session)))
    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))

    val res = testController.confirmRevokeAgentFromSome()(FakeRequest())

    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("some agent has been unassigned from your selected properties")
    page.html
      .getElementById("revoke-agent-summary-p1")
      .text() shouldBe "The agent can no longer act for you on any of the properties you selected."
    page.html
      .getElementById("revoke-agent-summary-p2")
      .text() shouldBe "The agent has not been removed from your account. They can still act for you if they add other properties to your account."
    page.html
      .getElementById("revoke-agent-summary-p3")
      .text() shouldBe "You can reassign an agent to a property if you want them to act for you again."
  }

  "show revoke agent summary page" should "render a success screen when all is well - in welsh" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    val agentName = "some agent"
    val session = RevokeAgentFromSomePropertiesSession(
      agentRevokeAction = Some(
        AgentRevokeBulkAction(
          agentCode = 123L,
          name = agentName,
          propertyLinkIds = List(),
          backLinkUrl = backLinkUrl.unsafeValue)))

    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(session)))
    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))

    val res = testController.confirmRevokeAgentFromSome()(welshFakeRequest)

    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText(s"Mae $agentName wedi’i ddadneilltuo o’r eiddo a ddewiswyd gennych")
    page.html
      .getElementById("revoke-agent-summary-p1")
      .text() shouldBe "Ni all yr asiant weithredu ar eich rhan mwyach ar unrhyw un o’r eiddo a ddewiswyd gennych."
    page.html
      .getElementById("revoke-agent-summary-p2")
      .text() shouldBe "Nid yw’r asiant wedi’i dynnu o’ch cyfrif. Gallant barhau i weithredu ar eich rhan os ydynt yn ychwanegu eiddo eraill at eich cyfrif."
    page.html
      .getElementById("revoke-agent-summary-p3")
      .text() shouldBe "Gallwch ailbennu asiant i eiddo os ydych am iddynt weithredu ar eich rhan eto."
  }

  "show revoke agent summary page" should "render a not found template when no agent data is cached" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))
    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))

    val res = testController.confirmRevokeAgentFromSome()(FakeRequest())

    status(res) shouldBe NOT_FOUND
  }

  "submitting an incomplete revoke agent form" should "re-render the page with form errors reported to user" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary(PaginationParameters(), agentCode)(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode" -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"      -> testAgentAccount.companyName,
        //"linkIds[]"   -> ...  OMIT linkIds to simulate bad form submission
        "backLinkUrl" -> backLinkUrl.unsafeValue
      ))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Select one or more properties")
    verifyPageErrorTitle(page)
  }

  "submitting an incomplete revoke agent form" should "re-render the page with form errors reported to user - in welsh" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(AgentAppointmentChangesResponse("id")))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary(PaginationParameters(), agentCode)(
      welshFakeRequest.withFormUrlEncodedBody(
        "agentCode" -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"      -> testAgentAccount.companyName,
        //"linkIds[]"   -> ...  OMIT linkIds to simulate bad form submission
        "backLinkUrl" -> backLinkUrl.unsafeValue
      ))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    verifyPageErrorTitle(page, isWelsh = true)
  }

  "errors during handling of address form" should "re-render the page with form errors reported to user" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(services.AppointRevokeException("something went awry")))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary(PaginationParameters(), agentCode)(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"        -> testAgentAccount.companyName,
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> backLinkUrl.unsafeValue
      ))
    status(res) shouldBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("There is a problem You must enter something to search for")
    verifyPageErrorTitle(page)
  }

  "errors during handling of  address form" should "re-render the page with form errors reported to user - in welsh" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.postAgentAppointmentChange(any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(services.AppointRevokeException("something went awry")))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary(PaginationParameters(), agentCode)(
      welshFakeRequest.withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"        -> testAgentAccount.companyName,
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> backLinkUrl.unsafeValue
      ))
    status(res) shouldBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Mae yna broblem Rhaid i chi nodi rhywbeth i chwilio amdano")
    verifyPageErrorTitle(page, isWelsh = true)
  }

  trait AppointToSomeConfirmationTestCase {
    self: RequestLang =>

    val agentAppointAction: AgentAppointBulkAction = AgentAppointBulkAction(
      agentSummary.representativeCode,
      agentSummary.name,
      propertyLinkIds = List.empty,
      backLinkUrl = "/back-link-url/unused-for/successful-appointments"
    )

    StubGroupAccountConnector.stubAccount(agent)
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any, any))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession(Some(agentAppointAction)))))

    val result: Future[Result] = testController.confirmAppointAgentToSome()(self.fakeRequest)
    val doc: Document = Jsoup.parse(contentAsString(result))

    val panel: String = doc.getElementsByTag("h1").text
    val explainer: String = doc.getElementById("explainer").text
    val nextStepsSubhead: String = doc.getElementById("next-steps-subhead").text
    val nextStepsContent: String = doc.getElementById("next-steps-content").text
    val accountHomeLink: Element = doc.getElementById("account-home-link")
  }

  private def verifyPageErrorTitle(page: HtmlPage, isWelsh: Boolean = false) =
    if (isWelsh)
      page.titleShouldMatch(
        s"Gwall: O ba eiddo ydych chi am ddadneilltuo $ggExternalId? - Valuation Office Agency - GOV.UK")
    else
      page.titleShouldMatch(
        s"Error: Which of your properties do you want to unassign $ggExternalId from? - Valuation Office Agency - GOV.UK")

  private lazy val testController = new AppointAgentController(
    errorHandler = mockCustomErrorHandler,
    accounts = StubGroupAccountConnector,
    authenticated = preAuthenticatedActionBuilders(),
    agentRelationshipService = mockAppointRevokeService,
    appointNewAgentSession = mockNewAgentSession,
    propertyLinksSessionRepo = mockSessionRepo,
    revokeAgentPropertiesSessionRepo = mockRevokeAgentPropertiesSessionRepo,
    appointAgentPropertiesSession = mockAppointAgentPropertiesSessionRepo,
    appointAgentSummaryView = appointAgentSummaryView,
    revokeAgentSummaryView = revokeAgentSummaryView,
    revokeAgentPropertiesView = revokeAgentPropertiesView,
    appointAgentPropertiesView = appointAgentPropertiesView
  )

  private lazy val mockNewAgentSession = mock[SessionRepo]
  private lazy val mockSessionRepo = mock[SessionRepo]
  private lazy val mockRevokeAgentPropertiesSessionRepo = mock[SessionRepo]
  private lazy val mockAppointAgentPropertiesSessionRepo = mock[SessionRepo]

  private lazy val mockAppointRevokeService = mock[AgentRelationshipService]

  type English = EnglishRequest
  type Welsh = WelshRequest

  trait UnfilteredResultsTestCase { self: RequestLang =>

    val testOwnerAuthResult: OwnerAuthResult = OwnerAuthResult(
      start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(ownerAuthorisation, ownerAuthorisation2, ownerAuthorisationWithNoAgent))

    lazy val ownerAuthResult: OwnerAuthResult = testOwnerAuthResult

    lazy val getMyOrganisationAgentsResponse: AgentList = organisationsAgentsListWithThreeAgents

    StubGroupAccountConnector.stubAccount(agent)
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any, any))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointRevokeService.getMyOrganisationAgents()(any))
      .thenReturn(Future.successful(getMyOrganisationAgentsResponse))
    when(mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any, any, any, any)(any))
      .thenReturn(Future.successful(ownerAuthResult))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate(any)(any, any)).thenReturn(Future.unit)
    when(mockSessionRepo.saveOrUpdate(any)(any, any)).thenReturn(Future.unit)

    val initialPaginationParams: PaginationParameters = PaginationParameters()
    val initialAgentAppointedQueryParam: Option[String] = None
    val backLinkQueryParam: RedirectUrl = RedirectUrl("http://localhost/some-back-link")
    lazy val result: Future[Result] = testController.getMyOrganisationPropertyLinksWithAgentFiltering(
      initialPaginationParams,
      agentCode,
      initialAgentAppointedQueryParam,
      backLinkQueryParam,
      false)(self.fakeRequest)

    val doc: Document = Jsoup.parse(contentAsString(result))
    val heading: String = doc.getElementsByTag("h1").text
    val explainerIntro: String = doc.getElementById("explainer-intro").text
    val explainerList: Element = doc.getElementById("explainer-list")

    private val searchFieldset: Element = doc.getElementById("search-fieldset")
    val searchLegend: String = searchFieldset.getElementsByTag("legend").text
    val addressInputLabel: String = searchFieldset.getElementById("address-input-label").text
    val agentSelectLabel: Option[String] = Option(searchFieldset.getElementById("agent-select-label")).map(_.text)
    val agentSelect: Option[Element] = Option(searchFieldset.getElementById("agent-select"))
    val selectableAgents: Option[mutable.Buffer[String]] = agentSelect.map(_.children.asScala.tail.map(_.text))
    val searchButton: String = searchFieldset.getElementById("search-submit").text
    val clearSearch: Element = searchFieldset.getElementById("clear-search")

    val selectAll: String = doc.getElementById("par-select-all-top").text
    val filterNoAgent: Element = doc.getElementById("filter-no-agent")

    private val resultsTable: Element = doc.getElementById("agentPropertiesTableBody")
    val sortByAddress: Element = resultsTable.getElementById("sort-by-address-link")
    val sortByAppointedAgents: Element = resultsTable.getElementById("sort-by-agent-link")
    val results: mutable.Seq[Element] = resultsTable.getElementsByTag("tbody").first.children.asScala
    val resultsAddresses: mutable.Seq[String] = results.map(_.getElementsByClass("govuk-checkboxes__label").first.text)
    val resultsAgents: mutable.Seq[mutable.Seq[String]] =
      results.map(_.getElementsByTag("td").last.children.asScala.map(_.text))

    val confirmButton: String = doc.getElementById("submit-button").text
    val cancelLink: Element = doc.getElementById("cancel-appoint")
  }

  trait FilterPropertiesToAppointEmptySearchTestCase { self: RequestLang =>
    val testOwnerAuth: OwnerAuthorisation =
      OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address 1", "localAuthorityRef", testAgents)
    val testOwnerAuthResult: OwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 2, total = 2, authorisations = Seq(testOwnerAuth))

    StubGroupAccountConnector.stubAccount(agent)
    when(mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any, any, any, any)(any))
      .thenReturn(Future.successful(testOwnerAuthResult))
    when(mockSessionRepo.saveOrUpdate(any)(any, any)).thenReturn(Future.unit)
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any, any))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[FilterAppointProperties](any)(any, any))
      .thenReturn(Future.unit)

    val errorResult: Future[Result] =
      testController.filterPropertiesForAppoint(PaginationParameters(), agentCode, None, backLinkUrl, false)(
        self.fakeRequest.withFormUrlEncodedBody("agentCode" -> "12345", "backLinkUrl" -> backLinkUrl.unsafeValue))
    val errorDoc: Document = Jsoup.parse(contentAsString(errorResult))

    val emptySearchError: Option[String] = Option(errorDoc.getElementsByAttributeValue("href", "#address")).map(_.text)
  }

}
