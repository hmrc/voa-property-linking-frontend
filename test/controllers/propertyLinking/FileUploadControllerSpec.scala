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

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import binders.propertylinks.EvidenceChoices
import controllers.VoaPropertyLinkingSpec
import models.{RatesBillType, StampDutyLandTaxForm}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status.{OK => _}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class FileUploadControllerSpec extends VoaPropertyLinkingSpec {

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
  override implicit val messagesControllerComponents: MessagesControllerComponents =
    app.injector.instanceOf[MessagesControllerComponents]
  lazy val mockBusinessRatesChallengeService = mock[BusinessRatesAttachmentsService]
  implicit lazy val request = FakeRequest().withHeaders(HOST -> "localhost:9523")
  implicit lazy val hc = HeaderCarrier()

  class TestFileUploadController(
        linkingSession: WithLinkingSession,
        authenticatedAction: AuthenticatedAction = preAuthenticatedActionBuilders())
      extends UploadController(
        mockCustomErrorHandler,
        authenticatedAction,
        linkingSession,
        mockBusinessRatesChallengeService,
        uploadRatesBillView,
        uploadEvidenceView,
        cannotProvideEvidenceView
      )
  lazy val linkingSession: WithLinkingSession = preEnrichedActionRefiner()
  def controller = new TestFileUploadController(linkingSession)

  "RATES_BILL file upload page" should "return valid page" in {
    val res = controller.show(EvidenceChoices.RATES_BILL, None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText(messages("uploadRatesBill.client.reminder"))
    html.shouldContain("#newFileGroup", 1)
  }

  "RATES_BILL file upload page" should "have a back link to the choose evidence page" in {
    val result =
      new TestFileUploadController(preEnrichedActionRefiner()).show(EvidenceChoices.RATES_BILL, None)(FakeRequest())
    val backLink: Element = Jsoup.parse(contentAsString(result)).getElementById("back-link")
    backLink.attr("href") shouldBe routes.ChooseEvidenceController.show().url
  }

  "RATES_BILL file initiate" should "return file upload initiate success" in {
    val request = FakeRequest(POST, "").withBody(
      Json.obj("fileName" -> "test.jpg", "mimeType" -> "image/jpeg", "evidenceType" -> RatesBillType.name))
    when(
      mockBusinessRatesChallengeService
        .initiateAttachmentUpload(any(), any())(any(), any[HeaderCarrier]))
      .thenReturn(Future.successful(preparedUpload))
    val result =
      controller.initiate(EvidenceChoices.RATES_BILL)(request)
    status(result) shouldBe OK
  }

  "RATES_BILL remove file" should "return remove file success" in {
    val request = FakeRequest(POST, "")
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = controller.remove("01222333", EvidenceChoices.RATES_BILL)(request)

    status(result) shouldBe SEE_OTHER
  }

  "RATES_BILL submit with no files uploaded" should "show error if no files selected" in {
    val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = controller.continue(EvidenceChoices.RATES_BILL)(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "RATES_BILL file upload with valid files" should "redirect to declaration page" in {
    lazy val linkingSessionWithAttachments: WithLinkingSession = preEnrichedActionRefiner(uploadEvidenceData)
    lazy val uploadController = new TestFileUploadController(linkingSessionWithAttachments)
    val request =
      fakeRequest.withHeaders(HOST -> "localhost:9523").withBody(Json.obj("evidenceType" -> "Lease"))
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = uploadController.continue(EvidenceChoices.RATES_BILL)(request)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  "OTHER Evidence file upload page" should "return valid page" in {
    val res = controller.show(EvidenceChoices.OTHER, None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText(messages("uploadOtherEvidence.title"))
    html.shouldContain("form input[type=radio]#evidenceType", 1)

  }

  "OTHER Evidence file initiate" should "return file upload initiate success" in {
    val request = FakeRequest(POST, "").withBody(
      Json.obj("fileName" -> "test.jpg", "mimeType" -> "image/jpeg", "evidenceType" -> StampDutyLandTaxForm.name))
    when(
      mockBusinessRatesChallengeService
        .initiateAttachmentUpload(any(), any())(any(), any[HeaderCarrier]))
      .thenReturn(Future.successful(preparedUpload))
    val result =
      controller.initiate(EvidenceChoices.OTHER)(request)
    status(result) shouldBe OK
  }

  "OTHER Evidence remove file" should "return remove file success" in {
    val request = FakeRequest(POST, "")
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = controller.remove("01222333", EvidenceChoices.OTHER)(request)

    status(result) shouldBe SEE_OTHER
  }

  "OTHER Evidence submit with no files uploaded" should "show error if no files selected" in {
    val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = controller.continue(EvidenceChoices.OTHER)(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "OTHER Evidence file upload with valid files" should "redirect to the declaration page" in {
    lazy val linkingSessionWithAttachments: WithLinkingSession = preEnrichedActionRefiner(uploadEvidenceData)
    lazy val uploadController = new TestFileUploadController(linkingSessionWithAttachments)
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = uploadController.continue(EvidenceChoices.OTHER)(
      FakeRequest().withFormUrlEncodedBody("evidenceType" -> "License"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  "OTHER Evidence page" should "return cannot provide evidence page for IP" in {
    lazy val linkingSession: WithLinkingSession = preEnrichedActionRefiner()
    lazy val uploadController =
      new TestFileUploadController(linkingSession, preAuthenticatedActionBuilders(userIsAgent = false))

    val result = uploadController.continue(EvidenceChoices.OTHER)(
      FakeRequest().withFormUrlEncodedBody("evidenceType" -> "unableToProvide"))
    status(result) shouldBe OK
    val html = HtmlPage(result)
    html.shouldContainText(messages("cannotProvideEvidence.title"))
    html.shouldContainText(messages("cannotProvideEvidence.p1"))
  }

  "OTHER Evidence page" should "return cannot provide evidence page for agent" in {
    lazy val linkingSession: WithLinkingSession = preEnrichedActionRefiner()
    lazy val uploadController = new TestFileUploadController(linkingSession)

    val result = uploadController.continue(EvidenceChoices.OTHER)(
      FakeRequest().withFormUrlEncodedBody("evidenceType" -> "unableToProvide"))
    status(result) shouldBe OK
    val html = HtmlPage(result)
    html.shouldContainText(messages("cannotProvideEvidence.title"))
    html.shouldContainText(messages("cannotProvideEvidence.agent.p1"))
  }

  "OTHER Evidence page" should "have a back link to the choose evidence page" in {
    val result =
      new TestFileUploadController(preEnrichedActionRefiner()).show(EvidenceChoices.OTHER, None)(FakeRequest())
    val backLink: Element = Jsoup.parse(contentAsString(result)).getElementById("back-link")
    backLink.attr("href") shouldBe routes.ChooseEvidenceController.show().url
  }

  "updateEvidenceType" should "show error if no evidence type submitted" in {
    val postRequest = FakeRequest(POST, "").withBody(Json.obj("evidenceType" -> ""))
    val result = controller.updateEvidenceType()(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "updateEvidenceType" should "return OK if valid evidence type submitted" in {
    when(mockBusinessRatesChallengeService.persistSessionData(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    val postRequest = FakeRequest(POST, "").withBody(Json.obj("evidenceType" -> "lease"))
    val result = controller.updateEvidenceType()(postRequest)

    status(result) shouldBe OK
  }
}
