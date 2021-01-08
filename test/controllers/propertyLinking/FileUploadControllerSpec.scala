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
import akka.stream.Materializer
import binders.propertylinks.EvidenceChoices
import controllers.VoaPropertyLinkingSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status.{OK => _}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class FileUploadControllerSpec extends VoaPropertyLinkingSpec {
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
  override implicit val messagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockBusinessRatesChallengeService = mock[BusinessRatesAttachmentsService]
  implicit lazy val request = FakeRequest().withSession(token).withHeaders(HOST -> "localhost:9523")
  implicit lazy val hc = HeaderCarrier()
  private val mockUploadRatesBillView = mock[views.html.propertyLinking.uploadRatesBill]
  private val mockUploadEvidenceView = mock[views.html.propertyLinking.uploadEvidence]
  class TestFileUploadController(linkingSession: WithLinkingSession)
      extends UploadController(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders(),
        linkingSession,
        mockBusinessRatesChallengeService,
        mockUploadRatesBillView,
        mockUploadEvidenceView
      )
  lazy val linkingSession: WithLinkingSession = preEnrichedActionRefiner()
  def controller = new TestFileUploadController(linkingSession)

  "call to file initiate" must "return file upload initiate success" in {
      val request = FakeRequest(POST, "").withBody(Json.obj("fileName" -> "test.jpg", "mimeType" -> "image/jpeg"))
      when(
        mockBusinessRatesChallengeService
          .initiateAttachmentUpload(any())(any(), any[HeaderCarrier]))
        .thenReturn(Future.successful(preparedUpload))
      val result =
        controller.initiate(EvidenceChoices.RATES_BILL)(request)
      status(result) mustBe OK
    }


  "call to remove file" must "return remove file success" in {
      val request = FakeRequest(POST, "")
      when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful())

      val result = controller.remove("01222333", EvidenceChoices.RATES_BILL)(request)

      status(result) mustBe SEE_OTHER
    }

  "call to continue with no files uploaded" must "show error if no files selected" in {
    when(mockUploadRatesBillView.apply(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(Html(""))

     val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = controller.continue(EvidenceChoices.RATES_BILL)(request)
      status(result) mustBe BAD_REQUEST
    }

  "call to continue with valid data" must "redirect to declaration page" in {
    lazy val linkingSessionWithAttachments: WithLinkingSession = preEnrichedActionRefiner(uploadEvidenceData)
    lazy val uploadController = new TestFileUploadController(linkingSessionWithAttachments)
    val request = fakeRequest.withSession(token).withHeaders(HOST -> "localhost:9523").withBody(Json.obj("evidenceType" -> "Lease"))
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful())

    val result = uploadController.continue(EvidenceChoices.RATES_BILL)(request)
    status(result) mustBe SEE_OTHER
  }
}
