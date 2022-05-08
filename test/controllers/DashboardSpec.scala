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

package controllers

import models.messages.Message
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRelationshipService
import utils._

import scala.concurrent.Future

class DashboardSpec extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()

  private val stubbedOwnerAuthResult: OwnerAuthResult =
    OwnerAuthResult(start = 1, total = 15, size = 15, filterTotal = 15, authorisations = Seq.empty[OwnerAuthorisation])

  lazy val mockRepService = {
    val m = mock[AgentRelationshipService]
    when(m.getMyOrganisationsPropertyLinks(any(), any())(any()))
      .thenReturn(Future.successful(stubbedOwnerAuthResult))
    m
  }

  object TestDashboard
      extends Dashboard(
        mockCustomErrorHandler,
        mockRepService,
        StubGroupAccountConnector,
        preAuthenticatedActionBuilders(),
        stubMessagesControllerComponents())

  "home page" should "redirect to new dashboard" in {
    val res = TestDashboard.home()(request)
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9542/business-rates-dashboard/home")
  }

  "viewMessages" should "redirect to new dashoard inbox" in {
    val res = TestDashboard.viewMessages()(request)

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9542/business-rates-dashboard/inbox")
  }

  "viewMessage" should "redirect to new dashoard inbox" in {
    val message = arbitrary[Message].sample.get

    val res = TestDashboard.viewMessage(message.id)(request)

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9542/business-rates-dashboard/inbox")
  }

}
