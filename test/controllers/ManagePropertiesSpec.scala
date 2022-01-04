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

import connectors.GroupAccounts
import models._
import models.searchApi._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRelationshipService
import utils._

class ManagePropertiesSpec extends VoaPropertyLinkingSpec {

  "The manage properties page" should "return redirect" in {

    setup()

    val res = TestDashboardController.manageProperties()(FakeRequest())
    status(res) shouldBe SEE_OTHER
  }

  private def setup(numberOfLinks: Int = 15) = {
    val groupAccount: GroupAccount = arbitrary[GroupAccount]

    var arbitraryOwnerAuthorisation: Seq[OwnerAuthorisation] = Nil

    (1 to numberOfLinks) foreach { _ =>
      arbitraryOwnerAuthorisation :+= arbitrary[OwnerAuthorisation].copy(authorisationId = groupAccount.id)
    }

    StubPropertyLinkConnector.stubOwnerAuthResult(
      OwnerAuthResult(
        start = 1,
        size = numberOfLinks,
        total = numberOfLinks,
        filterTotal = numberOfLinks,
        authorisations = arbitraryOwnerAuthorisation))
  }

  private object TestDashboardController
      extends Dashboard(
        mockCustomErrorHandler,
        mock[AgentRelationshipService],
        mock[GroupAccounts],
        preAuthenticatedActionBuilders(),
        stubMessagesControllerComponents()
      )
}
