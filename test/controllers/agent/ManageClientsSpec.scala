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

import controllers.VoaPropertyLinkingSpec
import models._
import models.searchApi.{AgentAuthClient, AgentAuthResult, AgentAuthorisation}
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._
import utils._

class ManageClientsSpec extends VoaPropertyLinkingSpec {

  "The manage clients page" should "return redirect" in {

    setup()

    val res = TestController.viewClientProperties()(FakeRequest())
    status(res) shouldBe SEE_OTHER
  }

  private def setup(numberOfLinks: Int = 15): Unit = {
    val groupAccount: GroupAccount = arbitrary[GroupAccount].copy(isAgent = true)
    var arbitraryAgentAuthorisation: Seq[AgentAuthorisation] = Nil

    (1 to numberOfLinks) foreach { _ =>
      arbitraryAgentAuthorisation :+= arbitrary[AgentAuthorisation].copy(
        authorisedPartyId = groupAccount.id,
        client = arbitrary[AgentAuthClient].copy(organisationId = groupAccount.id))
    }

    StubPropertyRepresentationConnector.stubAgentAuthResult(
      AgentAuthResult(
        start = 1,
        size = numberOfLinks,
        total = numberOfLinks,
        filterTotal = numberOfLinks,
        authorisations = arbitraryAgentAuthorisation))

  }

  object TestController
      extends RepresentationController(
        mockCustomErrorHandler,
        StubPropertyRepresentationConnector,
        mockVmvConnector,
        preAuthenticatedActionBuilders(),
        StubPropertyLinkConnector,
        revokeClientPropertyView,
        confirmRevokeClientPropertyView,
        stubMessagesControllerComponents()
      )

}
