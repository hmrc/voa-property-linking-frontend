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

package controllers.agentRevoke

import binders.pagination.PaginationParameters
import controllers.VoaPropertyLinkingSpec
import models.SessionPropertyLinks
import models.propertyrepresentation._
import models.searchApi._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import services.AgentRelationshipService
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.{HtmlPage, StubGroupAccountConnector}

import scala.concurrent.Future

class RevokeAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks with OptionValues {

  val agent = groupAccount(true).copy(agentCode = Some(agentCode))
  val backLinkUrl = RedirectUrl("http://localhost/some-back-link")
  val testAgents = Seq(OwnerAuthAgent(1L, agent.id, "organisationName", 1L))

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
        )
      )

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
        )
      )

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
        authorisations = Seq(testOwnerAuth, testOwnerAuth2)
      )

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
      )
    )

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
      )
    )

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
      )
    )

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/my-organisation/revoke/properties/confirm")
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
        // "linkIds[]"   -> ...  OMIT linkIds to simulate bad form submission
        "backLinkUrl" -> backLinkUrl.unsafeValue
      )
    )

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Select which properties you want to unassign this agent from")
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
        // "linkIds[]"   -> ...  OMIT linkIds to simulate bad form submission
        "backLinkUrl" -> backLinkUrl.unsafeValue
      )
    )

    status(res) shouldBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Dewiswch ba eiddo rydych chi am ddadaseinio’r asiant hwn oddi wrthynt")
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
      )
    )
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
      )
    )
    status(res) shouldBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.shouldContainText("Mae’n rhaid i chi nodi rhywbeth i chwilio amdano")
    verifyPageErrorTitle(page, isWelsh = true)
  }

  private def verifyPageErrorTitle(page: HtmlPage, isWelsh: Boolean = false) =
    if (isWelsh)
      page.titleShouldMatch(
        s"Gwall: O ba eiddo ydych chi am ddadneilltuo $ggExternalId? - Valuation Office Agency - GOV.UK"
      )
    else
      page.titleShouldMatch(
        s"Error: Which of your properties do you want to unassign $ggExternalId from? - Valuation Office Agency - GOV.UK"
      )

  private lazy val testController = new RevokeAgentController(
    errorHandler = mockCustomErrorHandler,
    accounts = StubGroupAccountConnector,
    authenticated = preAuthenticatedActionBuilders(),
    agentRelationshipService = mockAppointRevokeService,
    propertyLinksSessionRepo = mockSessionRepo,
    revokeAgentPropertiesSessionRepo = mockRevokeAgentPropertiesSessionRepo,
    revokeAgentSummaryView = revokeAgentSummaryView,
    revokeAgentPropertiesView = revokeAgentPropertiesView
  )

  private lazy val mockSessionRepo = mock[SessionRepo]
  private lazy val mockRevokeAgentPropertiesSessionRepo = mock[SessionRepo]

  private lazy val mockAppointRevokeService = mock[AgentRelationshipService]

  type English = EnglishRequest
  type Welsh = WelshRequest

}
