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
import models.LinkingSession
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
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

  private def testChooseEvidenceController(fromCya: Boolean = false) =
    new ChooseEvidenceController(
      mockCustomErrorHandler,
      preAuthenticatedActionBuilders(),
      if (fromCya) preEnrichedActionRefinerFromCya()
      else preEnrichedActionRefiner(),
      mockBusinessRatesAttachmentService,
      chooseEvidenceView
    )

  lazy val request = FakeRequest()

  "The choose evidence page with earliest start date in the past" should "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = testChooseEvidenceController().show()(request)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Do you have a copy of your client's business rates bill for this property? - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Do you have a copy of your client's business rates bill for this property?")
  }

  "The choose evidence page with earliest start date in the future" should "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val res = testChooseEvidenceController().show()(request)
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Do you have a copy of your client's business rates bill for this property? - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Do you have a copy of your client's business rates bill for this property? ")
  }

  it should "require the user to select whether they have a rates bill" in {

    val res = testChooseEvidenceController().submit()(request)
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: Do you have a copy of your client's business rates bill for this property? - Valuation Office Agency - GOV.UK")
    html.shouldContainText("There is a problem Select an option")
  }

  it should "redirect to the rates bill upload page if the user has a rates bill" in {
    val res = testChooseEvidenceController().submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.UploadController.show(EvidenceChoices.RATES_BILL).url)
  }

  it should "redirect to the other evidence page if the user does not have a rates bill" in {
    val res = testChooseEvidenceController().submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.UploadController.show(EvidenceChoices.OTHER).url)
  }

  it should "redirect to the rates bill upload page if the user has a rates bill, even if coming from CYA" in {
    val res = testChooseEvidenceController(fromCya = true)
      .submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.UploadController.show(EvidenceChoices.RATES_BILL).url)
  }

  it should "redirect to the other evidence page if the user does not have a rates bill, even if coming from CYA" in {
    val res = testChooseEvidenceController(fromCya = true)
      .submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.UploadController.show(EvidenceChoices.OTHER).url)
  }

  it should "have a back link to the property occupancy page" in {
    val doc: Document = Jsoup.parse(contentAsString(testChooseEvidenceController().show(request)))
    val backLink = doc.getElementById("back-link")
    backLink.attr("href") shouldBe routes.ClaimPropertyOccupancyController.showOccupancy().url
  }

// //  keeping this because it will need to be re-implemented after VTCCA-5189 is complete
//  it should "have a back link to the CYA page when coming from CYA" in {
//    val doc: Document = Jsoup.parse(contentAsString(testChooseEvidenceController(fromCya = true).show(request)))
//    doc.getElementById("back-link").attr("href") shouldBe routes.DeclarationController.show().url
//  }

  it should "set the fromCya session value to false on show" in {
    val mockAttachmentService = mock[BusinessRatesAttachmentsService]
    when(mockAttachmentService.persistSessionData(any())(any())).thenReturn(Future.successful(()))
    val controllerFromCya = new ChooseEvidenceController(
      mockCustomErrorHandler,
      preAuthenticatedActionBuilders(),
      preEnrichedActionRefinerFromCya(),
      mockAttachmentService,
      chooseEvidenceView
    )
    val sessionCaptor: ArgumentCaptor[LinkingSession] = ArgumentCaptor.forClass(classOf[LinkingSession])
    controllerFromCya.show(FakeRequest()).futureValue
    verify(mockAttachmentService).persistSessionData(sessionCaptor.capture())(any())
    sessionCaptor.getValue.fromCya shouldBe Some(false)
  }
}
