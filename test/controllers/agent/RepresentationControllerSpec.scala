/*
 * Copyright 2020 HM Revenue & Customs
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

  val revokeClientPropertyPage = mock[views.html.propertyrepresentation.revokeClient]

  object TestController
      extends RepresentationController(
        mockCustomErrorHandler,
        StubPropertyRepresentationConnector,
        preAuthenticatedActionBuilders(),
        StubPropertyLinkConnector,
        revokeClientPropertyPage,
        stubMessagesControllerComponents()
      )

  behavior of "revokeClientPropertyConfirmed method"
  it should "revoke an agent and redirect to the client properties page" in {
    when(revokeClientPropertyPage.apply(any())(any(), any(), any())).thenReturn(Html(""))
    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    StubPropertyLinkConnector.stubClientPropertyLink(clientProperty)
    val res =
      TestController.revokeClientPropertyConfirmed(clientProperty.submissionId)(request)

    status(res) must be(SEE_OTHER)
    redirectLocation(res) must be(Some(applicationConfig.newDashboardUrl("client-properties")))
  }

  behavior of "revokeClient method"
  it should "revoke an agent and display the revoke client page" in {
    when(revokeClientPropertyPage.apply(any())(any(), any(), any())).thenReturn(Html(""))
    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    StubPropertyLinkConnector.stubClientPropertyLink(clientProperty)
    val res = TestController.revokeClient(clientProperty.submissionId)(request)

    status(res) must be(OK)
  }
  it should "revoke an agent should return not found when clientPropertyLink cannot be found" in {
    when(mockCustomErrorHandler.notFoundTemplate(any()))
      .thenReturn(Html("NOT FOUND"))

    val res = TestController.revokeClient("some-id")(request)
    status(res) must be(NOT_FOUND)
  }

}
