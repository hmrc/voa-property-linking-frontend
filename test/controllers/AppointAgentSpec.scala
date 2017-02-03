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

import java.util.UUID

import connectors.Authenticated
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._

import scala.util.Random

class AppointAgentSpec extends ControllerSpec {
  import TestData._

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
    StubPropertyConnector.stubProperty(property)

    val res = TestAppointAgent.appoint(link.authorisationId)(request)
    status(res) must be (OK)

    val page = HtmlPage(res)
    page.mustContainTextInput("#agentCode_text")
    page.mustContainRadioSelect("canCheck", AgentPermission.options)
    page.mustContainRadioSelect("canChallenge", AgentPermission.options)
  }

  it must "require the user to enter an agent code" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(property)
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> "", "canCheck" -> "continueOnly", "canChallenge" -> "continueOnly")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("agentCode" -> "Numeric value expected")
  }

  it must "require the user to select agent permissions for checks" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(property)
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "", "canChallenge" -> "continueOnly")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canCheck" -> "No value selected")
  }

  it must "require the user to select agent permissions for challenges" in {
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(property)
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "continueOnly", "canChallenge" -> "")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canChallenge" -> "No value selected")
  }

  it must "not allow agents to be appointed with no permissions" in {
    pending
    stubLoggedInUser()
    StubPropertyConnector.stubProperty(property)
    StubGroupAccountConnector.stubAccount(agentAccount)
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
    StubPropertyConnector.stubProperty(property)
    StubGroupAccountConnector.stubAccount(agentAccount)
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAppointAgent.appointSubmit(link.authorisationId)(
      request.withFormUrlEncodedBody("agentCode" -> "not an agent", "canCheck" -> "continueOnly", "canChallenge" -> "notPermitted")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("agentCode" -> "Invalid agent code")
  }

  def stubLoggedInUser() = {
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubIndividualAccountConnector.stubAccount(individualAccount)
    StubAuthentication.stubAuthenticationResult(Authenticated(AccountIds(groupAccount.id, individualAccount.individualId)))
  }

  private object TestData {
    val groupAccount = GroupAccount(1, "2", "3", Address(None, "4", "", "", "", "AA56 7AA"), "89@01.23", "456", false, false, "")
    val individualAccount = DetailedIndividualAccount("externalId", "trustId", 1, 2, IndividualDetails(
      "FirstName", "LastName", "email@address.com", "12345", None, Address(None, "999", "The Place", "", "", "AB12 3CD")
    ))
    val property = Property(12345, "1234", PropertyAddress(Seq("123 Fake Street"), "AA1 1AA"), "123", "A building", "W")
    val account = GroupAccount(Random.nextInt(Int.MaxValue), "987654", "a company",
      Address(None, "123", "The Road", "", "", "AA11 1AA"), "aa@aa.aa", "1234", false, false, "")
    val agentAccount = GroupAccount(Random.nextInt(Int.MaxValue), "456789", "another company",
      Address(None, "123", "The Road", "", "", "AA11 1AA"), "bb@cc.dd", "1234", false, true, UUID.randomUUID().toString)
    val link = PropertyLink(6584351, property.uarn, account.id, "a thing", Nil, false, PropertyAddress(Seq("somewhere"), "AA12 4GS"),
      Capacity(OwnerOccupier, LocalDate.now(), None), DateTime.now(), true, Nil)
  }
}
