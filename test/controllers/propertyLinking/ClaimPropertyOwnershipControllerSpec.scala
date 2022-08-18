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
import uk.gov.hmrc.http.HeaderCarrier
import utils._
import java.time.LocalDate

import org.jsoup.Jsoup

import scala.concurrent.Future

class ClaimPropertyOwnershipControllerSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  private def testClaimProperty(earliestStartDate: LocalDate = earliestEnglishStartDate) =
    new ClaimPropertyOwnershipController(
      errorHandler = mockCustomErrorHandler,
      sessionRepository = mockSessionRepo,
      authenticatedAction = preAuthenticatedActionBuilders(),
      withLinkingSession = preEnrichedActionRefinerWithStartDate(earliestStartDate),
      ownershipToPropertyView = ownershipToPropertyView
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

    val res = testClaimProperty().showOwnership()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: When your client became the owner or occupier of the property - Valuation Office Agency - GOV.UK")
    html.shouldContainText("There is a problem")

  }

  "The claim ownership page with earliest start date in the future" should "return redirect to choose evidence page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testClaimProperty(earliestStartDate = LocalDate.now().plusYears(1)).showOwnership()(FakeRequest())
    status(res) shouldBe SEE_OTHER
  }

  "The claim ownership page on client behalf" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty().showOwnership()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: When your client became the owner or occupier of the property - Valuation Office Agency - GOV.UK")
    html.shouldContainText("There is a problem")

  }

  it should "reject invalid form submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    val res = testClaimProperty().submitOwnership()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))

    val res = testClaimProperty().submitOwnership()(
      FakeRequest().withFormUrlEncodedBody(
        "interestedOnOrBefore" -> "true",
        "stillInterested"      -> "true"
      ))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.ClaimPropertyOccupancyController.showOccupancy().url)
  }

}
