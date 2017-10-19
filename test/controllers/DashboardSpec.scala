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

import config.ApplicationConfig
import connectors._
import models._
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{StubPropertyLinkConnector, _}

import scala.concurrent.Future

class DashboardSpec extends ControllerSpec {
  implicit val request = FakeRequest()

  lazy val mockDraftCases = {
    val m = mock[DraftCases]
    when(m.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Nil))
    m
  }

  object TestDashboard extends Dashboard(
    app.injector.instanceOf[ApplicationConfig],
    mockDraftCases,
    StubPropertyLinkConnector,
    StubMessagesConnector,
    StubAuthentication
  )

  "Logging in with a non-organisation account" must "redirect to the wrong account type error page" in {
    StubAuthConnector.stubExternalId(shortString)
    StubAuthConnector.stubGroupId(shortString)
    StubAuthentication.stubAuthenticationResult(NonOrganisationAccount)

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Application.invalidAccountType().url)
  }

  "Logging in for the first time with a group account" must "redirect to the create individual account page" in {
    StubAuthConnector.stubExternalId("hasnoaccount")
    StubAuthConnector.stubGroupId("groupwithoutaccount")
    StubAuthentication.stubAuthenticationResult(NoVOARecord)
    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.CreateIndividualAccount.show.url)
  }

  "Logging in for the first time with an individual sub-account under a group that has registered" must "redirect to the create individual account page" in {
    StubAuthConnector.stubExternalId("hasnoaccount")
    StubAuthConnector.stubGroupId("hasgroupaccount")
    StubGroupAccountConnector.stubAccount(arbitrary[GroupAccount].sample.get.copy(groupId = "hasgroupaccount"))
    StubAuthentication.stubAuthenticationResult(NoVOARecord)

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.CreateIndividualAccount.show.url)
  }

  "Logging in again with an account that has already registered" must "continue to the manage properties page" in {
    val group = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val person = arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = "has-account", organisationId = group.id)

    StubAuthConnector.stubExternalId("has-account")
    StubAuthConnector.stubGroupId("has-group-account")
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

    StubAuthConnector.stubExternalId("has-account")
    StubAuthConnector.stubGroupId("has-agent-account")
    StubIndividualAccountConnector.stubAccount(person)
    StubGroupAccountConnector.stubAccount(group)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(group, person)))

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.agent.routes.RepresentationController.viewClientProperties().url)
  }
}
