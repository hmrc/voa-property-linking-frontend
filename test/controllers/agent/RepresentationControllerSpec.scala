/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.Authenticated
import controllers.ControllerSpec
import models.{Accounts, ClientProperty, GroupAccount}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import org.scalacheck.Arbitrary.arbitrary
import utils._

class RepresentationControllerSpec extends ControllerSpec {


  val request = FakeRequest().withSession(token)
  object TestRepresentationController extends RepresentationController {
    override val reprConnector = StubPropertyRepresentationConnector
    override val authenticated = StubAuthentication
    override val propertyLinkConnector = StubPropertyLinkConnector
  }

  behavior of "revokeClientConfirmed method"

  it should "revoke an agent and redirect to manage client page if the agent is not representing any more properties" in {
    val (agentGroupAccount, agentIndividual) = stubLoggedInUser()
    val  ownerOrgId = 111
    val permId = 1
    val clientProp = arbitrary[ClientProperty].sample.get
    StubPropertyLinkConnector.stubClientProperties(clientProp.copy(ownerOrganisationId = ownerOrgId, permissionId = permId))
    val res = TestRepresentationController.revokeClientConfirmed(ownerOrgId, permId)(request)
    redirectLocation(res) must be (Some(controllers.agent.routes.RepresentationController.manageRepresentationRequest().url))
    status(res) must be (SEE_OTHER)
  }
  it should "revoke an agent and redirect to view client properties page if the agent is representing more properties for the client" in {
    val (agentGroupAccount, agentIndividual) = stubLoggedInUser()
    val  ownerOrgId = 111
    val permIdOne = 1
    val permIdTwo = 2
    val clientProp = arbitrary[ClientProperty].sample.get
    StubPropertyLinkConnector.stubClientProperties(clientProp.copy(ownerOrganisationId = ownerOrgId, permissionId = permIdOne))
    StubPropertyLinkConnector.stubClientProperties(clientProp.copy(ownerOrganisationId = ownerOrgId, permissionId = permIdTwo))
    val res = TestRepresentationController.revokeClientConfirmed(ownerOrgId, permIdOne)(request)
    redirectLocation(res) must be (Some(controllers.routes.Dashboard.clientProperties(ownerOrgId).url))
    status(res) must be (SEE_OTHER)
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
