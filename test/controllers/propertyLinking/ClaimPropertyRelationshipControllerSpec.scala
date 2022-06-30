/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.propertyLinking

import binders.propertylinks.ClaimPropertyReturnToPage
import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class ClaimPropertyRelationshipControllerSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  private val mockRelationshipToPropertyView = mock[views.html.propertyLinking.relationshipToProperty]
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepository)

  private lazy val testClaimProperty = new ClaimPropertyRelationshipController(
    errorHandler = mockCustomErrorHandler,
    submissionIdConnector = StubSubmissionIdConnector,
    sessionRepository = mockSessionRepository,
    authenticatedAction = preAuthenticatedActionBuilders(),
    withLinkingSession = preEnrichedActionRefinerWithStartDate(earliestEnglishStartDate),
    propertyLinkingService = mockPropertyLinkingService,
    propertyLinksConnector = propertyLinkingConnector,
    vmvConnector = vmvConnector,
    runModeConfiguration = configuration,
    relationshipToPropertyView = mockRelationshipToPropertyView,
    beforeYouStartView = new views.html.propertyLinking.beforeYouStart(mainLayout, govukButton)
  )

  lazy val submissionId: String = shortString
  override val testAccounts: Accounts = arbitrary[Accounts]
  lazy val anEnvelopeId = java.util.UUID.randomUUID().toString

  lazy val propertyLinkingConnector = mock[PropertyLinkConnector]

  lazy val vmvConnector = {
    val vmvConnector = mock[VmvConnector]
    when(vmvConnector.getPropertyHistory(any())(any())).thenReturn(Future.successful(propertyHistory))
    vmvConnector
  }

  trait Setup {
    StubSubmissionIdConnector.stubId(submissionId)

    when(mockPropertyLinkingService.findEarliestStartDate(any()))
      .thenReturn(earliestEnglishStartDate)

    when(mockSessionRepository.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(mockSessionRepository.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
  }

  "The claim property relationship page" should "return valid page" in new Setup {
    when(mockRelationshipToPropertyView.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim property relationship page"))

    val res = testClaimProperty.showRelationship(positiveLong, rtp = ClaimPropertyReturnToPage.FMBR)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText("claim property relationship page")
  }

  "The claim property relationship page on client behalf" should "return valid page" in new Setup {
    when(mockRelationshipToPropertyView.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim property relationship page on client behalf"))

    val res = testClaimProperty
      .showRelationship(
        positiveLong,
        Some(ClientDetails(positiveLong, shortString)),
        rtp = ClaimPropertyReturnToPage.FMBR)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText("claim property relationship page on client behalf")
  }

  it should "contain link back to business-rates-find if that's where the request came from" in new Setup {
    val res =
      testClaimProperty.showRelationship(
        positiveLong,
        Some(ClientDetails(positiveLong, shortString)),
        rtp = ClaimPropertyReturnToPage.FMBR)(
        FakeRequest().withHeaders(
          ("referer", "http://localhost:9542/business-rates-find/summary/10361354?uarn=156039182")))

    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.contain("http://localhost:9542/business-rates-find/summary/10361354?uarn=156039182")
  }

  it should "initialise the linking session on show" in new Setup {
    when(mockRelationshipToPropertyView.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim property relationship page on client behalf"))

    val res = testClaimProperty
      .showRelationship(
        positiveLong,
        Some(ClientDetails(positiveLong, shortString)),
        rtp = ClaimPropertyReturnToPage.FMBR)(FakeRequest())
    status(res) shouldBe OK

    verify(mockSessionRepository).start(any())(any(), any())
  }

  it should "reject invalid form submissions" in new Setup {
    val res = testClaimProperty.submitRelationship(positiveLong)(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "redirect to the claim relationship page on valid submissions" in new Setup {
    when(mockSessionRepository.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testClaimProperty.submitRelationship(positiveLong)(
      FakeRequest().withFormUrlEncodedBody(
        "capacity" -> "OWNER"
      ))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.ClaimPropertyOwnershipController.showOwnership.url)
  }

  "show" should "redirect the user to vmv search for property page" in new Setup {
    val res = testClaimProperty.show()(FakeRequest())

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9300/business-rates-find/search")
  }
}
