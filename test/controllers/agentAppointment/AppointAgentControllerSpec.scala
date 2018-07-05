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

package controllers.agentAppointment

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{AgentsConnector, Authenticated, PropertyRepresentationConnector}
import controllers.VoaPropertyLinkingSpec
import models._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import utils.{StubAuthentication, StubGroupAccountConnector}

class AppointAgentControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  "appointMultipleProperties" should "do this" in {
    val res = testController.appointMultipleProperties()(FakeRequest())
    status(res) mustBe OK
  }

  private lazy val testController = new AppointAgentController(mockRepresentationConnector, StubGroupAccountConnector, mockPropertyLinkConnector, mockAgentsConnector, StubAuthentication, mockSessionRepo)

  private lazy val mockPropertyLinkConnector = mock[PropertyLinkConnector]

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockAgentsConnector = mock[AgentsConnector]

  private lazy val mockSessionRepo = mock[SessionRepo]

  private def stubLogin() = {
    val accounts = Accounts(groupAccountGen, individualGen)
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    (accounts.organisation, accounts.person)
  }
}
