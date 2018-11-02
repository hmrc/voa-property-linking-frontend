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

package controllers.agent

import connectors.{Authenticated, PropertyRepresentationConnector}
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

import scala.concurrent.Future

class RepresentationControllerSpec extends VoaPropertyLinkingSpec {

  lazy val request = FakeRequest().withSession(token)

  val mockPropertyRepresentationConnector = mock[PropertyRepresentationConnector]

  object TestRepresentationController extends RepresentationController(
    mockPropertyRepresentationConnector,
    StubAuthentication,
    StubPropertyLinkConnector,
    StubMessagesConnector
  )

  "confirm" should "allow the user to confirm that they want to reject the pending representation requests" in {
    stubLoggedInUser()
    val clientProperty: ClientProperty = arbitrary[ClientProperty]
    val propRep: PropertyRepresentation = arbitrary[PropertyRepresentation]
    val mockRepresentationBulkAction = mock[RepresentationBulkAction]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)

    val res = TestRepresentationController.confirm(1, 15)(request.withFormUrlEncodedBody(
      "page" -> "1",
      "pageSize" -> "15",
      "action" -> "reject",
      "requestIds[]" -> "1",
      "complete" -> "3"
    ))

    status(res) mustBe OK
  }

  "confirm" should "allow the user to confirm that they want to accept the pending representation requests" in {
    stubLoggedInUser()
    val clientProperty: ClientProperty = arbitrary[ClientProperty]
    val propRep: PropertyRepresentation = arbitrary[PropertyRepresentation]
    val propReps: PropertyRepresentations = PropertyRepresentations(1, Seq(propRep))
    val mockRepresentationBulkAction = mock[RepresentationBulkAction]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)
    when(mockPropertyRepresentationConnector.forAgent(any(),any(), any())).thenReturn(Future.successful(Proper))

    val res = TestRepresentationController.confirm(1, 15)(request.withFormUrlEncodedBody(
      "page" -> "1",
      "pageSize" -> "15",
      "action" -> "accept-confirm",
      "requestIds[]" -> "1",
      "complete" -> "3"
    ))

    status(res) mustBe OK
  }

  behavior of "revokeClientConfirmed method"
  it should "revoke an agent and redirect to the client properties page" in {
    stubLoggedInUser()
    val clientProperty: ClientProperty = arbitrary[ClientProperty]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)
    val res = TestRepresentationController.revokeClientConfirmed(clientProperty.authorisationId, clientProperty.ownerOrganisationId)(request)

    status(res) must be(SEE_OTHER)
    redirectLocation(res) must be(Some(applicationConfig.newDashboardUrl("client-properties")))
  }

  behavior of "revokeClient method"
  it should "revoke an agent and display the revoke client page" in {
    stubLoggedInUser()
    val clientProperty: ClientProperty = arbitrary[ClientProperty]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)
    val res = TestRepresentationController.revokeClient(clientProperty.authorisationId, clientProperty.ownerOrganisationId)(request)

    status(res) must be(OK)

    contentAsString(res) contains "Revoking client" mustBe true
    contentAsString(res) contains "Are you sure you no longer want to act on behalf of" mustBe true
  }
  it should "revoke an agent should return not found when clientProperty cannot be found" in {
    stubLoggedInUser()
    val res = TestRepresentationController.revokeClient(12L, 34L)(request)
    status(res) must be(NOT_FOUND)
  }

  def stubLoggedInUser() = {
    val groupAccount = groupAccountGen.sample.get.copy(isAgent = true)
    val individual = individualGen.sample.get
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubIndividualAccountConnector.stubAccount(individual)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individual)))
    (groupAccount, individual)
  }
}
