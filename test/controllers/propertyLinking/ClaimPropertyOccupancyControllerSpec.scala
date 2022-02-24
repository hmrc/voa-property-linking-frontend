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

import java.time.LocalDate

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
import utils._

import scala.concurrent.Future

class ClaimPropertyOccupancyControllerSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  private lazy val testClaimProperty = new ClaimPropertyOccupancyController(
    mockCustomErrorHandler,
    mockSessionRepo,
    preAuthenticatedActionBuilders(),
    preEnrichedActionRefiner(),
    occupanyOfPropertyPage,
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

  "The claim occupancy page with earliest start date in the past" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))

    val res = testClaimProperty.showOccupancy()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.verifyElementText("page-header", "Does your client still own or occupy the property?")
    html.verifyElementText("caption", "Add a property")
    html.mustContainRadioSelect("stillOccupied", Seq("true", "false"))
    html.shouldContainDateSelect("lastOccupiedDate")

  }

  "The claim occupancy page with earliest start date in the future" should "return redirect to choose evidence page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.now().plusYears(1))))

    val res = testClaimProperty.showOccupancy()(FakeRequest())
    status(res) shouldBe SEE_OTHER

  }

  "The claim occupancy page on client behalf" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))

    val res = testClaimProperty.showOccupancy()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.verifyElementText("page-header", "Does your client still own or occupy the property?")
    html.verifyElementText("caption", "Add a property")
    html.mustContainRadioSelect("stillOccupied", Seq("true", "false"))
    html.shouldContainDateSelect("lastOccupiedDate")

  }

  it should "reject invalid form submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))
    val res = testClaimProperty.submitOccupancy()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))
    val res = testClaimProperty.submitOccupancy()(
      FakeRequest().withFormUrlEncodedBody(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "23",
        "lastOccupiedDate.month" -> "4",
        "lastOccupiedDate.year"  -> "2017"
      ))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.ChooseEvidenceController.show().url)
  }

}
