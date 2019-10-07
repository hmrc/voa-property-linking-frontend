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

package controllers.agent

import connectors.authorisation.Authenticated
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

class RepresentationControllerSpec extends VoaPropertyLinkingSpec {

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(token)
  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestController extends RepresentationController(
    mockCustomErrorHandler,
    StubPropertyRepresentationConnector,
    preAuthenticatedActionBuilders(),
    StubPropertyLinkConnector
  )

  "cancel" should "allow the user to cancel accepting/rejecting the pending representation requests" in {
    StubPropertyLinkConnector.stubClientProperty(arbitrary[ClientProperty])

    val res = TestController.cancel(1, 15)(request.withFormUrlEncodedBody(
      "page" -> "1",
      "pageSize" -> "15",
      "action" -> "accept-confirm",
      "requestIds[]" -> "1",
      "complete" -> "3"
    ))

    status(res) mustBe OK
  }

  "cancel" should "throw Bad Request if the form has errors" in {
    StubPropertyLinkConnector.stubClientProperty(arbitrary[ClientProperty])

    val res = TestController.cancel(1, 15)(request.withFormUrlEncodedBody(
      "page" -> "",
      "pageSize" -> "",
      "action" -> "",
      "requestIds[]" -> "1",
      "complete" -> ""
    ))

    status(res) mustBe BAD_REQUEST
  }

  "continue" should "redirect the user to the manage clients page" in {
    StubPropertyLinkConnector.stubClientProperty(arbitrary[ClientProperty])

    val res = TestController.continue(1, 15)(request.withFormUrlEncodedBody(
      "page" -> "1",
      "pageSize" -> "15",
      "action" -> "accept",
      "requestIds[]" -> "1",
      "complete" -> "3"
    ))

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("/business-rates-property-linking/manage-clients")
  }

  "continue" should "throw Bad Request if the form has errors" in {
    StubPropertyLinkConnector.stubClientProperty(arbitrary[ClientProperty])
    when(mockCustomErrorHandler.badRequestTemplate(any()))
        .thenReturn(Html("BAD REQUEST"))

    val res = TestController.continue(1, 15)(request.withFormUrlEncodedBody(
      "page" -> "1",
      "pageSize" -> "",
      "action" -> "",
      "requestIds[]" -> "",
      "complete" -> "3"
    ))

    status(res) mustBe BAD_REQUEST
  }


  behavior of "revokeClientConfirmed method"
  it should "revoke an agent and redirect to the client properties page" in {
    val clientProperty: ClientProperty = arbitrary[ClientProperty]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)
    val res = TestController.revokeClientConfirmed(clientProperty.authorisationId, clientProperty.ownerOrganisationId)(request)

    status(res) must be(SEE_OTHER)
    redirectLocation(res) must be(Some(applicationConfig.newDashboardUrl("client-properties")))
  }

  behavior of "revokeClient method"
  it should "revoke an agent and display the revoke client page" in {
    val clientProperty: ClientProperty = arbitrary[ClientProperty]

    StubPropertyLinkConnector.stubClientProperty(clientProperty)
    val res = TestController.revokeClient(clientProperty.authorisationId, clientProperty.ownerOrganisationId)(request)

    status(res) must be(OK)

    contentAsString(res) contains "Revoking client" mustBe true
    contentAsString(res) contains "Are you sure you no longer want to act on behalf of" mustBe true
  }
  it should "revoke an agent should return not found when clientProperty cannot be found" in {
    when(mockCustomErrorHandler.notFoundTemplate(any()))
        .thenReturn(Html("NOT FOUND"))

    val res = TestController.revokeClient(12L, 34L)(request)
    status(res) must be(NOT_FOUND)
  }

}
