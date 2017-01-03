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

import auth.GGAction
import connectors.VPLAuthConnector
import models._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._

import scala.util.Random

class DashboardSpec extends ControllerSpec {
  implicit val request = FakeRequest()
  val testAddress = SimpleAddress(None, "123", "The Road", "", "", "AA11 1AA")

  object TestDashboard extends Dashboard {
    override val auth: VPLAuthConnector = StubAuthConnector
    override val ggAction: GGAction = StubGGAction
    override val individuals = StubIndividualAccountConnector
    override val groups = StubGroupAccountConnector
  }

  "Logging in for the first time with a group account" must
    "redirect to the create individual account page" in {
    StubAuthConnector.stubExternalId("hasnoaccount")
    StubAuthConnector.stubGroupId("groupwithoutaccount")
    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.CreateIndividualAccount.show.url)
  }

  "Logging in for the first time with an individual sub-account under a group that has registered" must "redirect to the create individual account page" in {
    StubAuthConnector.stubExternalId("hasnoaccount")
    StubAuthConnector.stubGroupId("hasgroupaccount")
    StubGroupAccountConnector.stubAccount(GroupAccount(Random.nextInt(Int.MaxValue), "hasgroupaccount", "", SimpleAddress(None, "123", "The Road", "", "", "AA11 1AA"), "", "", false, Some(UUID.randomUUID().toString)))

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.CreateIndividualAccount.show.url)
  }

  "Logging in again with an account that has already registered" must "continue to the dashboard" in {
    val groupId = Random.nextInt(Int.MaxValue)
    StubAuthConnector.stubExternalId("has-account")
    StubAuthConnector.stubGroupId("has-group-account")
    StubIndividualAccountConnector.stubAccount(IndividualAccount("has-account", UUID.randomUUID().toString, groupId, IndividualDetails("fname", "lname", "aa@aa.aa", "123", None, testAddress)))
    StubGroupAccountConnector.stubAccount(GroupAccount(groupId, "has-group-account", "", SimpleAddress(None, "123", "The Road", "", "", "AA11 1AA"), "", "", false, None))

    val res = TestDashboard.home()(request)
    status(res) mustBe OK
  }

  "Logging in with a group account that has registered as an agent" must "continue to the agent dashboard" in {
    val groupId = Random.nextInt(Int.MaxValue)
    StubAuthConnector.stubExternalId("has-account")
    StubAuthConnector.stubGroupId("has-agent-account")
    StubIndividualAccountConnector.stubAccount(IndividualAccount("has-account", UUID.randomUUID().toString, groupId, IndividualDetails("fname", "lname", "aa@aa.aa", "123", None, testAddress)))
    StubGroupAccountConnector.stubAccount(GroupAccount(groupId, "has-agent-account", "", SimpleAddress(None, "123", "The Road", "", "", "AA11 1AA"), "", "", false, Some(UUID.randomUUID().toString)))

    val res = TestDashboard.home()(request)
    status(res) mustBe OK
  }
}
