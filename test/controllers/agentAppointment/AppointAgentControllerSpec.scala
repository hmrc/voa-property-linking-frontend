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
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder}
import controllers.VoaPropertyLinkingSpec
import models.{AgentAppointBulkAction, AgentRevokeBulkAction, SessionPropertyLinks}
import models.propertyrepresentation.{AppointAgentToSomePropertiesSession, FilterAppointProperties, RevokeAgentFromSomePropertiesSession}
import models.searchApi._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import services.{AgentRelationshipService, AppointRevokeException}
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import utils.{HtmlPage, StubGroupAccountConnector}

import scala.concurrent.Future

class AppointAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  val agent = groupAccount(true).copy(agentCode = Some(agentCode))

  val testAgents = Seq(OwnerAuthAgent(1L, agent.id, "organisationName", 1L))

  "getMyOrganisationPropertyLinksWithAgentFiltering" should "show the appoint agent properties page" in {

    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

//    val sessionData = AppointAgentToSomeSession()

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

    val res = testController.getMyOrganisationPropertyLinksWithAgentFiltering(
      PaginationParameters(),
      agentCode,
      None,
      "/my-organisation/appoint")(FakeRequest())
    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
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

    val res = testController.paginatePropertiesForAppoint(
      PaginationParameters().nextPage,
      agentCode,
      None,
      "/my-organisation/appoint")(FakeRequest())
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
      ExternalPropertyLinkManagementSortField.ADDRESS,
      PaginationParameters(),
      agentCode,
      None,
      "/my-organisation/appoint")(FakeRequest())
    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContain("#sort-by-address.sort_desc", 1)
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

    val backLink = "/my-organisation/appoint"
    val res =
      testController.filterPropertiesForAppoint(PaginationParameters(), agentCode, None, backLink)(
        FakeRequest().withFormUrlEncodedBody(
          "address"     -> "address 1",
          "agentCode"   -> "12345",
          "backLinkUrl" -> backLink
        ))

    status(res) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("ADDRESS 1")
  }

  "filterPropertiesForAppoint" should "show error when nothing is entered" in {

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

    val backLink = "/my-organisation/appoint"
    val res =
      testController.filterPropertiesForAppoint(PaginationParameters(), agentCode, None, backLink)(
        FakeRequest().withFormUrlEncodedBody("agentCode" -> "12345", "backLinkUrl" -> backLink))

    status(res) shouldBe BAD_REQUEST

    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("You must enter something to search for")
    page.titleShouldMatch(
      s"Error: Appoint agent $ggExternalId to one or more properties - Valuation Office Agency - GOV.UK")
  }

  "filterPropertiesForAppoint" should "remember the last searched-for agent in the dropdown" in {

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

    val backLink = "/my-organisation/appoint"
    val res =
      testController.filterPropertiesForAppoint(PaginationParameters(), agentCode, None, backLink)(
        FakeRequest().withFormUrlEncodedBody(
          "address"     -> "address 1",
          "agent"       -> "Some Agent Org",
          "backLinkUrl" -> backLink
        ))

    status(res) shouldBe OK

    val page = Jsoup.parse(contentAsString(res))
    val dropdown = page.getElementById("agent")
    val firstOption = dropdown.child(0).text()

    firstOption shouldBe "Some Agent Org"
  }

  "appointAgentSummary" should "submit appoint agent request and redirect to summary page" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockAppointAgentPropertiesSessionRepo.saveOrUpdate[AppointAgentToSomePropertiesSession](any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testController.appointAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"           -> s"$agentCode",
        "name"                -> s"$companyName",
        "checkPermission"     -> "StartAndContinue",
        "challengePermission" -> "StartAndContinue",
        "linkIds[]"           -> "1",
        "backLinkUrl"         -> "/some/back/link"
      ))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/appoint/properties/confirm")

  }

  "appointAgentSummary" should "show the summary page" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    val agentAppointAction = AgentAppointBulkAction(
      agentCode = agentCode,
      name = companyName,
      propertyLinkIds = List(),
      backLinkUrl = "/some/back/link")
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any())).thenReturn(
      Future.successful(
        Some(
          AppointAgentToSomePropertiesSession(
            agentAppointAction = Some(agentAppointAction)
          ))))

    val res = testController.confirmAppointAgentToSome()(FakeRequest())

    status(res) shouldBe OK
    val document = Jsoup.parse(contentAsString(res))
    document
      .getElementById("mainBodyText")
      .text() shouldBe s"${agentAppointAction.name} will be able to discuss your property with the Valuation Office Agency on your behalf."

  }

  "appointAgentSummary" should "show not found  page when no agent data is cached" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockAppointAgentPropertiesSessionRepo.get[AppointAgentToSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(AppointAgentToSomePropertiesSession())))
    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))

    val res = testController.confirmAppointAgentToSome()(FakeRequest())

    status(res) shouldBe NOT_FOUND

  }

  "appointAgentSummary with form errors" should "show the appoint agent properties page" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = testController.appointAgentSummary()(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode", "backLinkUrl" -> "/some/back/link"))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
    page.titleShouldMatch(
      s"Error: Appoint agent $ggExternalId to one or more properties - Valuation Office Agency - GOV.UK")

  }

  "appointAgentSummary" should "show the appoint agent properties page when an appointment fails" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(AppointRevokeException("")))

    val res = testController.appointAgentSummary()(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "linkIds[]" -> "1", "backLinkUrl" -> "/some/back/link"))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainTable("#agentPropertiesTableBody")
    page.titleShouldMatch(
      s"Error: Appoint agent $ggExternalId to one or more properties - Valuation Office Agency - GOV.UK")

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
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = testController.revokeAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"        -> testAgentAccount.companyName,
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> "backlink url"
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
          backLinkUrl = "/some-back-url")))

    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(session)))
    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful((): Unit))

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

  "show revoke agent summary page" should "render a not found template when no agent data is cached" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))
    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful((): Unit))

    val res = testController.confirmRevokeAgentFromSome()(FakeRequest())

    status(res) shouldBe NOT_FOUND
  }

  "submitting an incomplete revoke agent form" should "re-render the page with form errors reported to user" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful((): Unit))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode" -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"      -> testAgentAccount.companyName,
        //"linkIds[]"   -> ...  OMIT linkIds to simulate bad form submission
        "backLinkUrl" -> "backlink url"
      ))

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Select one or more properties")
    verifyPageErrorTitle(page)
  }

  "errors during handling of revoke agent form" should "re-render the page with form errors reported to user" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(services.AppointRevokeException("something went awry")))
    when(mockRevokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession](any(), any()))
      .thenReturn(Future.successful(Some(RevokeAgentFromSomePropertiesSession())))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "name"        -> testAgentAccount.companyName,
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> "backlink url"
      ))
    status(res) shouldBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Failed to appoint agent to all properties")
    verifyPageErrorTitle(page)
  }

  private def verifyPageErrorTitle(page: HtmlPage) =
    page.titleShouldMatch(
      s"Error: Which of your properties do you want to unassign $ggExternalId from? - Valuation Office Agency - GOV.UK")

  private lazy val testController = new AppointAgentController(
    errorHandler = mockCustomErrorHandler,
    accounts = StubGroupAccountConnector,
    authenticated = preAuthenticatedActionBuilders(),
    agentRelationshipService = mockAppointRevokeService,
    propertyLinksSessionRepo = mockSessionRepo,
    revokeAgentPropertiesSessionRepo = mockRevokeAgentPropertiesSessionRepo,
    appointAgentPropertiesSession = mockAppointAgentPropertiesSessionRepo,
    revokeAgentSummaryView = revokeAgentSummaryView,
    appointAgentSummaryView = appointAgentSummaryView,
    revokeAgentPropertiesView = revokeAgentPropertiesView,
    appointAgentPropertiesView = appointAgentPropertiesView
  )

  private lazy val mockSessionRepo = mock[SessionRepo]
  private lazy val mockRevokeAgentPropertiesSessionRepo = mock[SessionRepo]
  private lazy val mockAppointAgentPropertiesSessionRepo = mock[SessionRepo]

  private lazy val mockAppointRevokeService = mock[AgentRelationshipService]

}
