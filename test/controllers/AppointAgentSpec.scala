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

package controllers

import connectors.Authenticated
import models._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._
import resources._
import org.scalacheck.Arbitrary.arbitrary

class AppointAgentSpec extends ControllerSpec {

  private object TestAppointAgent extends AppointAgentController {
    override val representations = StubPropertyRepresentationConnector
    override val properties = StubPropertyConnector
    override val accounts = StubGroupAccountConnector
    override val propertyLinks = StubPropertyLinkConnector
    override val authenticated = StubAuthentication
  }

  val request = FakeRequest().withSession(token)

  "The appoint a new agent page" must "allow the user to enter the agent code, and set permissions for checks and challenges" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(arbitrary[Property].sample.get)

    val res = TestAppointAgent.appoint(arbitrary[PropertyLink].sample.get.authorisationId)(request)
    status(res) must be (OK)

    val page = HtmlPage(res)
    page.mustContainTextInput("#agentCode_text")
    page.mustContainRadioSelect("canCheck", AgentPermission.options)
    page.mustContainRadioSelect("canChallenge", AgentPermission.options)
  }

  it must "require the user to enter an agent code" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(arbitrary[Property].sample.get)
    val groupAccount = arbitrary[GroupAccount].sample.get

    StubGroupAccountConnector.stubAccount(groupAccount)
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = groupAccount.id)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> "", "canCheck" -> "continueOnly", "canChallenge" -> "continueOnly")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("agentCode" -> "Numeric value expected")
  }

  it must "require the user to select agent permissions for checks" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(arbitrary[Property].sample.get)
    val agentAccount = arbitrary[GroupAccount].sample.get
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(arbitrary[PropertyLink].sample.get.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "", "canChallenge" -> "continueOnly")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canCheck" -> "No value selected")
  }

  it must "require the user to select agent permissions for challenges" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(arbitrary[Property].sample.get)
    val agentAccount = arbitrary[GroupAccount].sample.get
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(arbitrary[PropertyLink].sample.get.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "continueOnly", "canChallenge" -> "")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canChallenge" -> "No value selected")
  }

  it must "not allow agents to be appointed with no permissions" in {
    pending
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(arbitrary[Property].sample.get)
    val agentAccount = arbitrary[GroupAccount].sample.get
    StubGroupAccountConnector.stubAccount(agentAccount)
    val link = arbitrary[PropertyLink].sample.get
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "notPermitted", "canChallenge" -> "notPermitted")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canCheck" -> "Agent must either have permission to continue checks or challenges")
  }

  it must "require the agent code to be valid" in {
    pending
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(arbitrary[Property].sample.get)
    StubGroupAccountConnector.stubAccount(arbitrary[GroupAccount].sample.get)
    val link = arbitrary[PropertyLink].sample.get
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> "not an agent", "canCheck" -> "continueOnly", "canChallenge" -> "notPermitted")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("agentCode" -> "Invalid agent code")
  }

  def stubLoggedInUser() = {
    val groupAccount = groupAccountGen.sample.get
    val individual = individualGen.sample.get
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubIndividualAccountConnector.stubAccount(individual)
    StubAuthentication.stubAuthenticationResult(Authenticated(AccountIds(groupAccount.id, individual.individualId)))
  }
  
}
