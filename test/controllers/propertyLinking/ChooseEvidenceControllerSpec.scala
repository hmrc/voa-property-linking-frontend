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

import binders.propertylinks.EvidenceChoices
import controllers.VoaPropertyLinkingSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.HtmlPage

import java.time.LocalDate
import scala.concurrent.Future

class ChooseEvidenceControllerSpec extends VoaPropertyLinkingSpec {

  lazy val mockBusinessRatesAttachmentService = {
    val m = mock[BusinessRatesAttachmentsService]
    when(m.persistSessionData(any())(any())).thenReturn(Future.successful((): Unit))
    m
  }

  private val mockChooseEvidencePage = mock[views.html.propertyLinking.chooseEvidence]

  private object TestChooseEvidence
      extends ChooseEvidenceController(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders(),
        preEnrichedActionRefiner(),
        mockBusinessRatesAttachmentService,
        mockPropertyLinkingService,
        mockChooseEvidencePage
      )

  lazy val request = FakeRequest().withSession(token)

  "The choose evidence page with earliest start date in the past" must "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockChooseEvidencePage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("The choose evidence page"))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.of(2017, 4, 1))))

    val res = TestChooseEvidence.show()(request)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.mustContainText("The choose evidence page")
  }

  "The choose evidence page with earliest start date in the future" must "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockChooseEvidencePage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("The choose evidence page"))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.now().plusYears(1))))

    val res = TestChooseEvidence.show()(request)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.mustContainText("The choose evidence page")
  }

  it must "require the user to select whether they have a rates bill" in {
    when(mockChooseEvidencePage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("require the user to select whether they have a rates bill"))
    when(mockPropertyLinkingService.findEarliestStartDate(any())(any()))
      .thenReturn(Future.successful(Some(LocalDate.now().plusYears(1))))

    val res = TestChooseEvidence.submit()(request)
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainText("require the user to select whether they have a rates bill")
  }

  it must "redirect to the rates bill upload page if the user has a rates bill" in {
    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) shouldBe SEE_OTHER
    header("location", res) shouldBe Some(routes.UploadController.show(EvidenceChoices.RATES_BILL).url)
  }

  it must "redirect to the other evidence page if the user does not have a rates bill" in {

    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) shouldBe SEE_OTHER
    header("location", res) shouldBe Some(routes.UploadController.show(EvidenceChoices.OTHER).url)
  }

}
