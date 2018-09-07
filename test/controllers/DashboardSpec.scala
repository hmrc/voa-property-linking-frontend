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

package controllers

import com.builtamont.play.pdf.PdfGenerator
import connectors._
import models._
import models.messages.{Message, MessageCount, MessagePagination, MessageSearchResults}
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class DashboardSpec extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()

  override val additionalAppConfig = Seq("featureFlags.newDashboardRedirectsEnabled" -> "false")

  lazy val mockDraftCases = {
    val m = mock[DraftCases]
    when(m.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Nil))
    m
  }

  object TestDashboard extends Dashboard(
    mockDraftCases,
    StubPropertyLinkConnector,
    StubMessagesConnector,
    StubAgentConnector,
    StubGroupAccountConnector,
    StubAuthentication,
    mock[PdfGenerator]
  )

  "Logging in for the first time with a group account" must "redirect to the create individual account page" in {
    StubVplAuthConnector.stubExternalId("hasnoaccount")
    StubVplAuthConnector.stubGroupId("groupwithoutaccount")
    StubAuthentication.stubAuthenticationResult(NoVOARecord)
    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(registration.routes.RegistrationController.show.url)
  }

  "Logging in for the first time with an individual sub-account under a group that has registered" must "redirect to the create individual account page" in {
    StubVplAuthConnector.stubExternalId("hasnoaccount")
    StubVplAuthConnector.stubGroupId("hasgroupaccount")
    StubGroupAccountConnector.stubAccount(arbitrary[GroupAccount].sample.get.copy(groupId = "hasgroupaccount"))
    StubAuthentication.stubAuthenticationResult(NoVOARecord)

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(registration.routes.RegistrationController.show.url)
  }

  "Logging in again with an account that has already registered" must "continue to the manage properties page" in {
    val group = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val person = arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = "has-account", organisationId = group.id)

    StubVplAuthConnector.stubExternalId("has-account")
    StubVplAuthConnector.stubGroupId("has-group-account")
    StubIndividualAccountConnector.stubAccount(person)
    StubGroupAccountConnector.stubAccount(group)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(group, person)))

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Dashboard.manageProperties().url)
  }

  "Logging in with a group account that has registered as an agent" must "continue to the agent dashboard" in {
    val group = arbitrary[GroupAccount].sample.get.copy(groupId = "has-agent-account", isAgent = true)
    val person = arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = "has-account", organisationId = group.id)

    StubVplAuthConnector.stubExternalId("has-account")
    StubVplAuthConnector.stubGroupId("has-agent-account")
    StubIndividualAccountConnector.stubAccount(person)
    StubGroupAccountConnector.stubAccount(group)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(group, person)))

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.agent.routes.RepresentationController.viewClientProperties().url)
  }

  "viewManagedProperties" must "display the properties managed by an agent" in {
    val clientGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val clientPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = clientGroup.id)

    val agentGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = true, companyName = "Test Agent Company")
    val agentPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = agentGroup.id)

    StubVplAuthConnector.stubExternalId(clientPerson.externalId)
    StubVplAuthConnector.stubGroupId(clientGroup.groupId)

    StubIndividualAccountConnector.stubAccount(clientPerson)
    StubIndividualAccountConnector.stubAccount(agentPerson)

    StubGroupAccountConnector.stubAccount(clientGroup)
    StubGroupAccountConnector.stubAccount(agentGroup)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(clientGroup, clientPerson)))

    val res = TestDashboard.viewManagedProperties(agentGroup.agentCode)(request)

    status(res) mustBe OK
    contentAsString(res) must include("Properties managed by Test Agent Company")
  }

  "viewMessages" must "display the messages page with messages" in {
    val clientGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val clientPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = clientGroup.id)

    val messageSearchResults = arbitrary[MessageSearchResults].sample.get

    StubVplAuthConnector.stubExternalId(clientPerson.externalId)
    StubVplAuthConnector.stubGroupId(clientGroup.groupId)

    StubIndividualAccountConnector.stubAccount(clientPerson)
    StubGroupAccountConnector.stubAccount(clientGroup)

    StubMessagesConnector.stubMessageSearchResults(messageSearchResults)
    StubMessagesConnector.stubMessageCount(MessageCount(unread = messageSearchResults.size, total =  messageSearchResults.size))

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(clientGroup, clientPerson)))

    val res = TestDashboard.viewMessages(MessagePagination())(request)

    status(res) mustBe OK
    contentAsString(res) must include(messageSearchResults.messages.head.subject)
  }

  "viewMessage" must "display a message using the message id" in {
    val clientGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val clientPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = clientGroup.id)

    val message = arbitrary[Message].sample.get

    StubVplAuthConnector.stubExternalId(clientPerson.externalId)
    StubVplAuthConnector.stubGroupId(clientGroup.groupId)

    StubIndividualAccountConnector.stubAccount(clientPerson)
    StubGroupAccountConnector.stubAccount(clientGroup)

    StubMessagesConnector.stubMessage(message)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(clientGroup, clientPerson)))

    val res = TestDashboard.viewMessage(message.id)(request)

    status(res) mustBe OK
    contentAsString(res) must include(message.subject)
  }

}
