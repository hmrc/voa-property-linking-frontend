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

import actions.propertylinking.WithLinkingSession
import binders.propertylinks.EvidenceChoices
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import utils._
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.HtmlPage

import scala.concurrent.Future

class ChooseEvidenceControllerSpec extends VoaPropertyLinkingSpec {

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }
  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentsService]
  private val mockChooseEvidencePage = mock[views.html.propertyLinking.chooseEvidence]
  private class TestChooseEvidence(withLinkingSession: WithLinkingSession)
      extends ChooseEvidenceController(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders(),
        preEnrichedActionRefiner(),
        mockBusinessRatesAttachmentService,
        mockChooseEvidencePage) {
    val property = testProperty
  }

  private lazy val testChooseEvidence = new TestChooseEvidence(mockWithLinkingSession)

  lazy val testProperty: Property = arbitrary[Property]

  lazy val request = FakeRequest().withSession(token)

  "The choose evidence page" must "ask the user whether they have a rates bill" in {
    when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockChooseEvidencePage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("The choose evidence page"))

    val res = testChooseEvidence.show()(request)
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("The choose evidence page")
  }

  it must "require the user to select whether they have a rates bill" in {
    when(mockChooseEvidencePage.apply(any(), any())(any(), any(), any()))
      .thenReturn(Html("require the user to select whether they have a rates bill"))
    val res = testChooseEvidence.submit()(request)
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainText("require the user to select whether they have a rates bill")
  }

  it must "redirect to the rates bill upload page if the user has a rates bill" in {
    val res = testChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadController.show(EvidenceChoices.RATES_BILL).url)
  }

  it must "redirect to the other evidence page if the user does not have a rates bill" in {

    val res = testChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadController.show(EvidenceChoices.OTHER).url)
  }

}
