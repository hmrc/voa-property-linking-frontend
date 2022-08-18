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

import scala.concurrent.Future

class ChooseEvidenceControllerSpec extends VoaPropertyLinkingSpec {

  lazy val mockBusinessRatesAttachmentService = {
    val m = mock[BusinessRatesAttachmentsService]
    when(m.persistSessionData(any())(any())).thenReturn(Future.successful((): Unit))
    m
  }

  private object TestChooseEvidence
      extends ChooseEvidenceController(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders(),
        preEnrichedActionRefiner(),
        mockBusinessRatesAttachmentService,
        chooseEvidenceView
      )

  lazy val request = FakeRequest()

  "The choose evidence page with earliest start date in the past" should "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = TestChooseEvidence.show()(request)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Do you have a copy of your client's business rates bill for this property? - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Do you have a copy of your client's business rates bill for this property?")
  }

  "The choose evidence page with earliest start date in the future" should "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = TestChooseEvidence.show()(request)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Do you have a copy of your client's business rates bill for this property? - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Do you have a copy of your client's business rates bill for this property? ")
  }

  it should "require the user to select whether they have a rates bill" in {

    val res = TestChooseEvidence.submit()(request)
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: Do you have a copy of your client's business rates bill for this property? - Valuation Office Agency - GOV.UK")
    html.shouldContainText("There is a problem Select an option")
  }

  it should "redirect to the rates bill upload page if the user has a rates bill" in {
    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) shouldBe SEE_OTHER
    header("location", res) shouldBe Some(routes.UploadController.show(EvidenceChoices.RATES_BILL).url)
  }

  it should "redirect to the other evidence page if the user does not have a rates bill" in {

    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) shouldBe SEE_OTHER
    header("location", res) shouldBe Some(routes.UploadController.show(EvidenceChoices.OTHER).url)
  }

}
