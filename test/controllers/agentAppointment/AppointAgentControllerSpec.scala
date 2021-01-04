/*
 * Copyright 2021 HM Revenue & Customs
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
import binders.propertylinks.GetPropertyLinksParameters
import controllers.VoaPropertyLinkingSpec
import models.searchApi._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
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

    when(
      mockAppointRevokeService.getMyOrganisationPropertyLinksWithAgentFiltering(any(), any(), any(), any())(
        any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    when(mockSessionRepo.saveOrUpdate(any)(any(), any())).thenReturn(Future.successful(()))

    StubGroupAccountConnector.stubAccount(agent)

    val res = testController.getMyOrganisationPropertyLinksWithAgentFiltering(
      PaginationParameters(),
      GetPropertyLinksParameters(),
      agentCode,
      None,
      "/my-organisation/appoint")(FakeRequest())
    status(res) mustBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#agentPropertiesTableBody")
  }

  "appointAgentSummary" should "show the summary page" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = testController.appointAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"           -> s"$agentCode",
        "checkPermission"     -> "StartAndContinue",
        "challengePermission" -> "StartAndContinue",
        "linkIds[]"           -> "1",
        "backLinkUrl"         -> "/some/back/link"
      ))

    status(res) mustBe OK

  }

  "appointAgentSummary with form errors" should "show the appoint agent properties page" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = testController.appointAgentSummary()(
      FakeRequest().withFormUrlEncodedBody("agentCode" -> s"$agentCode", "backLinkUrl" -> "/some/back/link"))

    status(res) mustBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#agentPropertiesTableBody")

  }

  "appointAgentSummary" should "show the appoint agent properties page when an appointment fails" in {
    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.createAndSubmitAgentRepRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(AppointRevokeException("")))

    val res = testController.appointAgentSummary()(
      FakeRequest()
        .withFormUrlEncodedBody("agentCode" -> s"$agentCode", "linkIds[]" -> "1", "backLinkUrl" -> "/some/back/link"))

    status(res) mustBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainTable("#agentPropertiesTableBody")

  }

  "selectAgentPropertiesSearchSort" should "show a list of properties available for removal from a agent" in {
    val testOwnerAuth = OwnerAuthorisation(1L, "APPROVED", "1111111", 1L, "address", "localAuthorityRef", testAgents)

    val testOwnerAuthResult =
      OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = Seq(testOwnerAuth))

    StubGroupAccountConnector.stubAccount(agent)

    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    val res =
      testController.selectAgentPropertiesSearchSort(PaginationParameters(), GetPropertyLinksParameters(), agentCode)(
        FakeRequest().withFormUrlEncodedBody(
          "agentCode"      -> s"$agentCode",
          "agentCodeRadio" -> "1"
        ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText(testOwnerAuth.address)
  }

  "viewing select agent properties search sort page" should "show no properties when the agent has no properties appointed" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    val testOwnerAuthResult = OwnerAuthResult(start = 1, size = 15, filterTotal = 0, total = 0, authorisations = Seq())

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(testOwnerAuthResult))

    val res = testController.selectAgentPropertiesSearchSort(PaginationParameters(), GetPropertyLinksParameters(), 1L)(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"      -> "1",
        "agentCodeRadio" -> "1"
      ))

    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText("There are no properties to display")
  }

  "revoke agent summary page" should "render a success screen when all is well" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful((): Unit))

    val res = testController.revokeAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> "backlink url"
      ))

    status(res) mustBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText("You have removed Agent")
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
        //"linkIds[]"   -> ...  OMIT linkIds to simulate bad form submission
        "backLinkUrl" -> "backlink url"
      ))

    status(res) mustBe BAD_REQUEST

    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText("Select one or more properties")
  }

  "errors during handling of revoke agent form" should "re-render the page with form errors reported to user" in {
    val testAgentAccount = groupAccount(true).copy(agentCode = Some(1L))

    StubGroupAccountConnector.stubAccount(testAgentAccount)
    when(mockAppointRevokeService.createAndSubmitAgentRevokeRequest(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.failed(services.AppointRevokeException("something went awry")))
    when(mockAppointRevokeService.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(ownerAuthResultResponse))

    val res = testController.revokeAgentSummary()(
      FakeRequest().withFormUrlEncodedBody(
        "agentCode"   -> testAgentAccount.agentCode.fold("0")(_.toString),
        "linkIds[]"   -> ownerAuthorisation.submissionId,
        "backLinkUrl" -> "backlink url"
      ))
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainText("Failed to appoint agent to all properties")
  }

  private lazy val testController = new AppointAgentController(
    mockCustomErrorHandler,
    StubGroupAccountConnector,
    preAuthenticatedActionBuilders(),
    mockAppointRevokeService,
    mockSessionRepo
  )

  private lazy val mockSessionRepo = mock[SessionRepo]

  private lazy val mockAppointRevokeService = mock[AgentRelationshipService]

}
