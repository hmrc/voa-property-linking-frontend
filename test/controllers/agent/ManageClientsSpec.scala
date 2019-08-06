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

package controllers.agent

import config.ApplicationConfig
import connectors.Authenticated
import controllers.VoaPropertyLinkingSpec
import models._
import models.searchApi.{AgentAuthClient, AgentAuthResult, AgentAuthorisation}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import resources._
import utils._

import scala.collection.JavaConverters._

class ManageClientsSpec extends VoaPropertyLinkingSpec {

  "The manage clients page" must "return redirect" in {

    setup()

    val res = TestController.viewClientProperties()(FakeRequest())
    status(res) mustBe SEE_OTHER
  }

  private def setup(numberOfLinks: Int = 15): Unit = {
    val groupAccount: GroupAccount = arbitrary[GroupAccount].copy(isAgent = true)
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]
    var arbitraryAgentAuthorisation: Seq[AgentAuthorisation] = Nil

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))
    (1 to numberOfLinks) foreach { _ =>
      arbitraryAgentAuthorisation :+= arbitrary[AgentAuthorisation].copy(authorisedPartyId = groupAccount.id.toLong,
        client = arbitrary[AgentAuthClient].copy(organisationId = groupAccount.id.toLong))
    }

    StubPropertyRepresentationConnector.stubAgentAuthResult(AgentAuthResult(start = 1,
      size = numberOfLinks,
      total = numberOfLinks,
      filterTotal = numberOfLinks,
      authorisations = arbitraryAgentAuthorisation))

  }

  object TestController extends RepresentationController(
    StubPropertyRepresentationConnector,
    StubAuthentication,
    StubPropertyLinkConnector
  )

}