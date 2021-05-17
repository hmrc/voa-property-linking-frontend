/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.propertyLinking.PropertyLinkConnector
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{HtmlPage, StubSubmissionIdConnector, StubWithLinkingSession, _}

import java.time.LocalDate
import scala.concurrent.Future

class ClaimPropertyOwnershipControllerSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentsService]
  private val mockOwnershipToPropertyPage = mock[views.html.propertyLinking.ownershipToProperty]
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  private lazy val testClaimProperty = new ClaimPropertyOwnershipController(
    mockCustomErrorHandler,
    StubSubmissionIdConnector,
    mockSessionRepo,
    preAuthenticatedActionBuilders(),
    preEnrichedActionRefiner(),
    propertyLinkingConnector,
    mockBusinessRatesAttachmentService,
    configuration,
    mockOwnershipToPropertyPage,
    serviceUnavailableView = new views.html.errors.serviceUnavailable(mainLayout),
    mockPropertyLinkingService
  )

  lazy val submissionId: String = shortString
  override val testAccounts: Accounts = arbitrary[Accounts]
  lazy val anEnvelopeId = java.util.UUID.randomUUID().toString

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  lazy val propertyLinkingConnector = mock[PropertyLinkConnector]

  "The claim ownership page with earliest start date in the past" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockOwnershipToPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim ownership page loaded"))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))

    val res = testClaimProperty.showOwnership()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("claim ownership page loaded")

  }

  "The claim ownership page with earliest start date in the future" should "return redirect to choose evidence page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockBusinessRatesAttachmentService.persistSessionData(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockOwnershipToPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim ownership page loaded"))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.now().plusYears(1))))

    val res = testClaimProperty.showOwnership()(FakeRequest())
    status(res) mustBe SEE_OTHER

  }

  "The claim ownership page on client behalf" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockOwnershipToPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim ownership page on client behalf"))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))

    val res = testClaimProperty
      .showOwnership()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("claim ownership page on client behalf")

  }

  it should "reject invalid form submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    val res = testClaimProperty.submitOwnership()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockBusinessRatesAttachmentService.persistSessionData(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    val res = testClaimProperty.submitOwnership()(
      FakeRequest().withFormUrlEncodedBody(
        "interestedBefore2017" -> "true",
        "stillInterested"      -> "true"
      ))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.ChooseEvidenceController.show.url)
  }

}
