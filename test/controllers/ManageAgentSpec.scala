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

import connectors.{Authenticated, VPLAuthConnector}
import models._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._

class ManageAgentSpec extends ControllerSpec {
  implicit val request = FakeRequest()

  object TestDashboardController extends Dashboard {
    override val auth: VPLAuthConnector = StubAuthConnector
    override val individuals = StubIndividualAccountConnector
    override val groups = StubGroupAccountConnector
    override val authenticated = StubAuthentication
  }


  "Manage Agents page" must "return Ok" in {
    val organisationId = arbitrary[Int].sample.get
    val personId = arbitrary[Int].sample.get

    StubAuthentication.stubAuthenticationResult(Authenticated(AccountIds(organisationId, personId)))

    val res = TestDashboardController.manageAgents()(FakeRequest())
    status(res) mustBe OK
  }

}
