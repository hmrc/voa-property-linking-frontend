/*
 * Copyright 2019 HM Revenue & Customs
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

import models.DetailedIndividualAccount
import models.registration.UserInfo
import org.scalacheck.Arbitrary._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.auth.core.{Admin, AffinityGroup, User}
import utils.{StubGgAction, StubIndividualAccountConnector, StubVplAuthConnector}

import scala.concurrent.Future

class KeepAliveControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  val testIndividualInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Individual,
    gatewayId = "",
    credentialRole = User)

  val testOrganisationInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Organisation,
    gatewayId = "",
    credentialRole = Admin)

  val testAgentInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Agent,
    gatewayId = "",
    credentialRole = Admin)

  val messagesApi  = app.injector.instanceOf[MessagesApi]
  private object TestRegistrationController$ extends KeepAliveController(
    StubGgAction
  )

  "Keep Alive User Session" should
    "return keep alive returns 200" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testIndividualInfo)
    StubIndividualAccountConnector.stubAccount(arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = externalId))
    val res = TestRegistrationController$.keepAlive()(FakeRequest())
    status(res) mustBe OK
  }

}
