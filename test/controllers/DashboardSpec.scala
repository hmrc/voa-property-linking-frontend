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

package controllers

import connectors._
import models._
import models.messages.Message
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import services.AgentRelationshipService
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DashboardSpec extends VoaPropertyLinkingSpec with AllMocks {
  implicit val request = FakeRequest()

  lazy val mockDraftCases = {
    val m = mock[DraftCases]
    when(m.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Nil))
    m
  }

  private val stubbedOwnerAuthResult: OwnerAuthResult =
    OwnerAuthResult(start = 1, total = 15, size = 15, filterTotal = 15, authorisations = Seq.empty[OwnerAuthorisation])

  lazy val mockRepService = {
    val m = mock[AgentRelationshipService]
    when(m.getMyOrganisationsPropertyLinks(any(), any(), any())(any()))
      .thenReturn(Future.successful(stubbedOwnerAuthResult))
    m
  }

  object TestDashboard
      extends Dashboard(
        mockCustomErrorHandler,
        mockDraftCases,
        mockRepService,
        StubAgentConnector,
        StubGroupAccountConnector,
        preAuthenticatedActionBuilders(),
        stubMessagesControllerComponents()
      )

  "home page" must "redirect to new dashboard" in {
    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9542/business-rates-dashboard/home")
  }

  "viewManagedProperties" must "display the properties managed by an agent" in {
    val clientGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val clientPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = clientGroup.id)

    val agentGroup = arbitrary[GroupAccount].sample.get
      .copy(isAgent = true, companyName = "Test Agent Company", agentCode = Some(100000))
    val agentPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = agentGroup.id)

    StubIndividualAccountConnector.stubAccount(clientPerson)
    StubIndividualAccountConnector.stubAccount(agentPerson)

    StubGroupAccountConnector.stubAccount(clientGroup)
    StubGroupAccountConnector.stubAccount(agentGroup)

    val res = TestDashboard.viewManagedProperties(agentGroup.agentCode.get, false)(request)

    status(res) mustBe OK
    contentAsString(res) must include("Properties managed by Test Agent Company")
  }

  "viewMessages" must "redirect to new dashoard inbox" in {
    val res = TestDashboard.viewMessages()(request)

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9542/business-rates-dashboard/inbox")
  }

  "viewMessage" must "redirect to new dashoard inbox" in {
    val message = arbitrary[Message].sample.get

    val res = TestDashboard.viewMessage(message.id)(request)

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9542/business-rates-dashboard/inbox")
  }

}
