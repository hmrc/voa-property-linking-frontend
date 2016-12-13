/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.CapacityDeclaration
import models._
import org.joda.time.DateTime
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
    override val withAuthentication = StubWithAuthentication
    override val propertyLinks = StubPropertyLinkConnector
  }

  val request = FakeRequest().withSession(token)

  "The appoint a new agent page" must "allow the user to enter the agent code, and set permissions for checks and challenges" in {
    StubPropertyConnector.stubProperty(property)
    StubWithAuthentication.stubAuthentication(account)

    val res = TestAppointAgent.appoint(link.linkId)(request)
    status(res) must be (OK)

    val page = HtmlPage(res)
    page.mustContainTextInput("#agentCode_text")
    page.mustContainRadioSelect("canCheck", AgentPermissions.options)
    page.mustContainRadioSelect("canChallenge", AgentPermissions.options)
  }

  it must "require the user to enter an agent code" in {
    StubPropertyConnector.stubProperty(property)
    StubWithAuthentication.stubAuthentication(account)
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(link.linkId)(
      request.withFormUrlEncodedBody("agentCode" -> "", "canCheck" -> "continueOnly", "canChallenge" -> "continueOnly")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("agentCode" -> "This field is required")
  }

  it must "require the user to select agent permissions for checks" in {
    StubPropertyConnector.stubProperty(property)
    StubWithAuthentication.stubAuthentication(account)
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(link.linkId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "", "canChallenge" -> "continueOnly")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canCheck" -> "No value selected")
  }

  it must "require the user to select agent permissions for challenges" in {
    StubPropertyConnector.stubProperty(property)
    StubWithAuthentication.stubAuthentication(account)
    StubGroupAccountConnector.stubAccount(agentAccount)

    val res = TestAppointAgent.appointSubmit(link.linkId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "continueOnly", "canChallenge" -> "")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canChallenge" -> "No value selected")
  }

  it must "not allow agents to be appointed with no permissions" in {
    StubPropertyConnector.stubProperty(property)
    StubWithAuthentication.stubAuthentication(account)
    StubGroupAccountConnector.stubAccount(agentAccount)
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAppointAgent.appointSubmit(link.linkId)(
      request.withFormUrlEncodedBody("agentCode" -> agentAccount.groupId, "canCheck" -> "notPermitted", "canChallenge" -> "notPermitted")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("canCheck" -> "Agent must either have permission to continue checks or challenges")
  }

  it must "require the agent code to be valid" in {
    StubPropertyConnector.stubProperty(property)
    StubWithAuthentication.stubAuthentication(account)
    StubGroupAccountConnector.stubAccount(agentAccount)
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAppointAgent.appointSubmit(link.linkId)(
      request.withFormUrlEncodedBody("agentCode" -> "not an agent", "canCheck" -> "continueOnly", "canChallenge" -> "notPermitted")
    )
    status(res) must be (BAD_REQUEST)

    val page = HtmlPage(res)
    page.mustContainFieldErrors("agentCode" -> "Invalid agent code")
  }

  private object TestData {
    val property = Property(12345, "1234", PropertyAddress("123 Fake Street", "", "", "AA1 1AA"), false, "123", "A building", "W")
    val account = GroupAccount(Random.nextInt(Int.MaxValue), "987654", "a company", SimpleAddress(None, "123", "The Road", "", "", "AA11 1AA"), "aa@aa.aa", "1234", false, None)
    val agentAccount = GroupAccount(Random.nextInt(Int.MaxValue), "456789", "another company", SimpleAddress(None, "123", "The Road", "", "", "AA11 1AA"), "bb@cc.dd", "1234", false, Some(UUID.randomUUID().toString))
    val link = PropertyLink("6584351", property.uarn, account.groupId, "a thing", Capacity(OwnerOccupier, DateTime.now(), None), DateTime.now(), true)
  }
}
