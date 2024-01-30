/*
 * Copyright 2023 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class ClaimPropertyRelationshipControllerSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepository)

  private def testClaimProperty(userIsAgent: Boolean = true) = new ClaimPropertyRelationshipController(
    errorHandler = mockCustomErrorHandler,
    submissionIdConnector = StubSubmissionIdConnector,
    sessionRepository = mockSessionRepository,
    authenticatedAction = preAuthenticatedActionBuilders(),
    withLinkingSession = preEnrichedActionRefinerWithStartDate(earliestEnglishStartDate, userIsAgent),
    propertyLinkingService = mockPropertyLinkingService,
    propertyLinksConnector = propertyLinkingConnector,
    vmvConnector = vmvConnector,
    runModeConfiguration = configuration,
    relationshipToPropertyView = relationshipToPropertyView,
    claimPropertyStartView = claimPropertyStartView
  )

  private lazy val testClaimPropertyFromCya = new ClaimPropertyRelationshipController(
    errorHandler = mockCustomErrorHandler,
    submissionIdConnector = StubSubmissionIdConnector,
    sessionRepository = mockSessionRepository,
    authenticatedAction = preAuthenticatedActionBuilders(),
    withLinkingSession = preEnrichedActionRefinerFromCya(),
    propertyLinkingService = mockPropertyLinkingService,
    propertyLinksConnector = propertyLinkingConnector,
    vmvConnector = vmvConnector,
    runModeConfiguration = configuration,
    relationshipToPropertyView = relationshipToPropertyView,
    claimPropertyStartView = claimPropertyStartView
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

  it should "initialise the linking session on show" in new Setup {

    val res = testClaimProperty()
      .showStart(positiveLong, Some(ClientDetails(positiveLong, shortString)), rtp = ClaimPropertyReturnToPage.FMBR)(
        FakeRequest())
    status(res) shouldBe OK

    verify(mockSessionRepository).start(any())(any(), any())
  }

  it should "have a back link to the CYA page when coming from CYA after an invalid submission" in new Setup {
    val result: Future[Result] = testClaimPropertyFromCya.submitRelationship()(FakeRequest())
    status(result) shouldBe BAD_REQUEST
    val document: Document = Jsoup.parse(contentAsString(result))
    document.getElementById("back-link").attr("href") shouldBe routes.DeclarationController.show.url
  }

  "show" should "redirect the user to vmv search for property page" in new Setup {
    val res = testClaimProperty().show()(FakeRequest())

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9300/business-rates-find/search")
  }
}
