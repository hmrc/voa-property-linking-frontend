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

import auth.GGAction
import connectors.VPLAuthConnector
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{StubAccountConnector, StubAuthConnector, StubGGAction}

class DashboardSpec extends ControllerSpec {
  implicit val request = FakeRequest()

  object TestDashboard extends Dashboard {
    override val auth: VPLAuthConnector = StubAuthConnector
    override val ggAction: GGAction = StubGGAction
    override val accounts = StubAccountConnector
  }

  "Logging in with for the first time with a group account" must "redirect to the create individual account page" in {
    StubAccountConnector.reset()
    StubAuthConnector.stubInternalId("hasnoaccount")

    val res = TestDashboard.home()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.CreateIndividualAccount.show.url)
  }

  "Logging in again with an individual sub-account that has already registered" must "continue to the dashboard" in {
    StubAccountConnector.reset()
    StubAuthConnector.stubInternalId("has-account")
    StubAccountConnector.stubAccount(Account("has-account", false))

    val res = TestDashboard.home()(request)
    status(res) mustBe OK
  }
}
