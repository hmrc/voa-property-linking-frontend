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

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import binders.propertylinks.EvidenceChoices
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import controllers.VoaPropertyLinkingSpec
import models.{CapacityType, Occupier, Owner, RatesBillType, StampDutyLandTaxForm, UploadEvidenceData}
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

class UploadControllerSpec extends VoaPropertyLinkingSpec {
  lazy val mockBusinessRatesChallengeService = mock[BusinessRatesAttachmentsService]
  override implicit val messagesControllerComponents: MessagesControllerComponents =
    app.injector.instanceOf[MessagesControllerComponents]
  lazy val linkingSession: WithLinkingSession = preEnrichedActionRefiner()
  implicit lazy val request = FakeRequest().withHeaders(HOST -> "localhost:9523")
  implicit lazy val hc = HeaderCarrier()

  def agentController = new TestFileUploadController(linkingSession)

  def ipController(relationshipCapacity: CapacityType = Owner) =
    new TestFileUploadController(
      preEnrichedActionRefiner(
        evidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
        userIsAgent = false,
        relationshipCapacity = Some(relationshipCapacity)))

  "RATES_BILL file initiate" should "return file upload initiate success" in {
    val request = FakeRequest(POST, "").withBody(
      Json.obj("fileName" -> "test.jpg", "mimeType" -> "image/jpeg", "evidenceType" -> RatesBillType.name))
    when(
      mockBusinessRatesChallengeService
        .initiateAttachmentUpload(any(), any())(any(), any[HeaderCarrier]))
      .thenReturn(Future.successful(preparedUpload))
    val result =
      agentController.initiate(EvidenceChoices.RATES_BILL)(request)
    status(result) shouldBe OK
  }

  "RATES_BILL remove file" should "return remove file success" in {
    val request = FakeRequest(POST, "")
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = agentController.remove("01222333", EvidenceChoices.RATES_BILL)(request)

    status(result) shouldBe SEE_OTHER
  }

  "OTHER Evidence file upload page" should "display expected error message when upscan returns file to large error" in {
    val res =
      agentController.show(EvidenceChoices.OTHER, Some("Your proposed upload exceeds the maximum allowed size"))(
        FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldNotContainText("Your proposed upload exceeds the maximum allowed size")
    html.shouldContainText("The selected file must be smaller than 10MB")

  }

  "OTHER Evidence file initiate" should "return file upload initiate success" in {
    val request = FakeRequest(POST, "").withBody(
      Json.obj("fileName" -> "test.jpg", "mimeType" -> "image/jpeg", "evidenceType" -> StampDutyLandTaxForm.name))
    when(
      mockBusinessRatesChallengeService
        .initiateAttachmentUpload(any(), any())(any(), any[HeaderCarrier]))
      .thenReturn(Future.successful(preparedUpload))
    val result =
      agentController.initiate(EvidenceChoices.OTHER)(request)
    status(result) shouldBe OK
  }

  "OTHER Evidence remove file" should "return remove file success" in {
    val request = FakeRequest(POST, "")
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = agentController.remove("01222333", EvidenceChoices.OTHER)(request)

    status(result) shouldBe SEE_OTHER
  }

  "updateEvidenceType" should "show error if no evidence type submitted" in {
    val postRequest = FakeRequest(POST, "").withBody(Json.obj("evidenceType" -> ""))
    val result = agentController.updateEvidenceType()(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "updateEvidenceType" should "return OK if valid evidence type submitted" in {
    when(mockBusinessRatesChallengeService.persistSessionData(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    val postRequest = FakeRequest(POST, "").withBody(Json.obj("evidenceType" -> "lease"))
    val result = agentController.updateEvidenceType()(postRequest)

    status(result) shouldBe OK
  }

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  class TestFileUploadController(
        linkingSession: WithLinkingSession,
        authenticatedAction: AuthenticatedAction = preAuthenticatedActionBuilders())
      extends UploadController(
        mockCustomErrorHandler,
        authenticatedAction,
        linkingSession,
        mockBusinessRatesChallengeService,
        uploadRatesBillLeaseOrLicenseView,
        uploadEvidenceView,
        cannotProvideEvidenceView
      )
}
