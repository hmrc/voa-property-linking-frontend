/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.ControllerSpec
import models.{Accounts, ClientProperty}
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

class RepresentationControllerSpec extends ControllerSpec {

  lazy val request = FakeRequest().withSession(token)

  object TestRepresentationController extends RepresentationController(
    StubPropertyRepresentationConnector,
    StubAuthentication,
    StubPropertyLinkConnector,
    StubMessagesConnector
  )

  behavior of "revokeClientConfirmed method"

  it should "revoke an agent and redirect to the client properties page" in {
    stubLoggedInUser()
    val clientProperty: ClientProperty = arbitrary[ClientProperty]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)
    val res = TestRepresentationController.revokeClientConfirmed(clientProperty.authorisationId, clientProperty.ownerOrganisationId)(request)

    status(res) must be(SEE_OTHER)
    redirectLocation(res) must be(Some(routes.RepresentationController.viewClientProperties().url))
  }

  def stubLoggedInUser() = {
    val groupAccount = groupAccountGen.sample.get.copy(isAgent = true)
    val individual = individualGen.sample.get
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubIndividualAccountConnector.stubAccount(individual)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individual)))
    (groupAccount, individual)
  }
}
