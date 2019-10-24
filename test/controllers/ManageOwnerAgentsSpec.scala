/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRelationshipService
import utils._

class ManageOwnerAgentsSpec extends VoaPropertyLinkingSpec {

  implicit val request = FakeRequest()

  object TestDashboardController extends Dashboard(
    mockCustomErrorHandler,
    mock[DraftCases],
    mock[AgentRelationshipService],
    StubAgentConnector,
    mock[GroupAccounts],
    preAuthenticatedActionBuilders(),
    stubMessagesControllerComponents()
  )

  "Manage Owner Agents page" must "return redirect" in {
    val res = TestDashboardController.manageAgents()(FakeRequest())
    status(res) mustBe SEE_OTHER
  }

}
