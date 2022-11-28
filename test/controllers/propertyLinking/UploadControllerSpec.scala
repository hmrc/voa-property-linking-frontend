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
        uploadRatesBillLeaseOrLicenseView,
        uploadEvidenceView,
        cannotProvideEvidenceView
      )
  lazy val linkingSession: WithLinkingSession = preEnrichedActionRefiner()
  def agentController = new TestFileUploadController(linkingSession)
  def ipController(relationshipCapacity: CapacityType = Owner) =
    new TestFileUploadController(
      preEnrichedActionRefiner(
        evidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
        userIsAgent = false,
        relationshipCapacity = Some(relationshipCapacity)))

  "RATES_BILL file upload page" should "return valid page with correct back link - agent" in {
    testShowRatesBillLeaseOrLicense(
      evidenceChoice = EvidenceChoices.RATES_BILL,
      expectedPageHeader = "Upload your client's business rates bill")
  }
  "LEASE file upload page" should "return valid page with correct back link - agent" in {
    testShowRatesBillLeaseOrLicense(
      evidenceChoice = EvidenceChoices.LEASE,
      expectedPageHeader = "Upload your client's lease")
  }
  "LICENSE file upload page" should "return valid page with correct back link - agent" in {
    testShowRatesBillLeaseOrLicense(
      evidenceChoice = EvidenceChoices.LICENSE,
      expectedPageHeader = "Upload your client's licence to occupy property")
  }
  "RATES_BILL file upload page" should "return valid page with correct back link - IP" in {
    testShowRatesBillLeaseOrLicense(
      uploadController = ipController(),
      evidenceChoice = EvidenceChoices.RATES_BILL,
      expectedPageHeader = "Upload your business rates bill")
  }
  "LEASE file upload page" should "return valid page with correct back link - IP" in {
    testShowRatesBillLeaseOrLicense(
      uploadController = ipController(),
      evidenceChoice = EvidenceChoices.LEASE,
      expectedPageHeader = "Upload your lease")
  }
  "LICENSE file upload page" should "return valid page with correct back link - IP" in {
    testShowRatesBillLeaseOrLicense(
      uploadController = ipController(),
      evidenceChoice = EvidenceChoices.LICENSE,
      expectedPageHeader = "Upload your licence to occupy property")
  }

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

  "RATES_BILL submit with no files uploaded" should "show error if no files selected" in {
    val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = agentController.continue(EvidenceChoices.RATES_BILL)(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "LEASE Evidence submit with no files uploaded" should "show error if no files selected" in {
    val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = agentController.continue(EvidenceChoices.LEASE)(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "LICENSE Evidence submit with no files uploaded" should "show error if no files selected" in {
    val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = agentController.continue(EvidenceChoices.LICENSE)(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "RATES_BILL file upload with valid files" should "redirect to declaration page" in {
    testContinueWithRatesBillLeaseOrLicense(EvidenceChoices.RATES_BILL, "ratesBill")
  }

  "LEASE file upload with valid files" should "redirect to declaration page" in {
    testContinueWithRatesBillLeaseOrLicense(EvidenceChoices.LEASE, "lease")
  }

  "LICENSE file upload with valid files" should "redirect to declaration page" in {
    testContinueWithRatesBillLeaseOrLicense(EvidenceChoices.LICENSE, "license")
  }

  "OTHER Evidence file upload page" should "return valid page" in {
    val res = agentController.show(EvidenceChoices.OTHER, None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText(messages("uploadOtherEvidence.title"))
    html.shouldContain("form input[type=radio]#evidenceType", 1)

    val radioOptions = html.html.getElementsByClass("govuk-radios__item")
    radioOptions.size() shouldBe 8
    radioOptions.get(0).text() shouldBe "Lease"
    radioOptions.get(1).text() shouldBe "Licence to occupy"
    radioOptions.get(2).text() shouldBe "Service charge statement"
    radioOptions.get(3).text() shouldBe "Stamp Duty Land Tax form"
    radioOptions.get(4).text() shouldBe "Land Registry title"
    radioOptions.get(5).text() shouldBe "Water rate demand"
    radioOptions.get(6).text() shouldBe "Utility bill"
    radioOptions.get(7).text() shouldBe "I cannot provide evidence"
  }

  "NO_LEASE_OR_LICENSE Evidence file upload page" should "return valid page" in {
    val res =
      ipController(relationshipCapacity = Occupier).show(EvidenceChoices.NO_LEASE_OR_LICENSE, None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText(messages("uploadOtherEvidence.title"))
    html.shouldContain("form input[type=radio]#evidenceType", 1)

    val radioOptions = html.html.getElementsByClass("govuk-radios__item")
    radioOptions.size() shouldBe 7
    radioOptions.get(0).text() shouldBe "Business rates bill"
    radioOptions.get(1).text() shouldBe "Service charge statement"
    radioOptions.get(2).text() shouldBe "Stamp Duty Land Tax form"
    radioOptions.get(3).text() shouldBe "Land Registry title"
    radioOptions.get(4).text() shouldBe "Water rate demand"
    radioOptions.get(5).text() shouldBe "Utility bill"
    radioOptions.get(6).text() shouldBe "I cannot provide evidence"
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

  "OTHER Evidence submit with no files uploaded" should "show error if no files selected" in {
    val postRequest = fakeRequest.withFormUrlEncodedBody()
    val result = agentController.continue(EvidenceChoices.OTHER)(postRequest)
    status(result) shouldBe BAD_REQUEST
  }

  "OTHER Evidence file upload with valid files" should "redirect to the declaration page" in {
    lazy val linkingSessionWithAttachments: WithLinkingSession = preEnrichedActionRefiner(uploadServiceChargeData)
    lazy val uploadController = new TestFileUploadController(linkingSessionWithAttachments)
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = uploadController.continue(EvidenceChoices.OTHER)(
      FakeRequest().withFormUrlEncodedBody("evidenceType" -> "License"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  "RATES_BILL Evidence file upload with valid files" should "redirect to the declaration page" in {
    lazy val linkingSessionWithAttachments: WithLinkingSession = preEnrichedActionRefiner(uploadRatesBillData)
    lazy val uploadController = new TestFileUploadController(linkingSessionWithAttachments)
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = uploadController.continue(EvidenceChoices.OTHER)(
      FakeRequest().withFormUrlEncodedBody("evidenceType" -> RatesBillType.name))
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

  private def testShowRatesBillLeaseOrLicense(
        uploadController: TestFileUploadController = agentController,
        evidenceChoice: EvidenceChoices,
        expectedPageHeader: String) = {
    val res = uploadController.show(evidenceChoice, None)(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.html.getElementById("page-header").text() shouldBe expectedPageHeader
    html.shouldContain("#newFileGroup", 1)
    val backLink: Element = html.html.getElementById("back-link")
    backLink.attr("href") shouldBe routes.ChooseEvidenceController.show().url
  }

  private def testContinueWithRatesBillLeaseOrLicense(evidenceChoice: EvidenceChoices, evidenceType: String) = {
    lazy val linkingSessionWithAttachments: WithLinkingSession = preEnrichedActionRefiner(uploadRatesBillData)
    lazy val uploadController = new TestFileUploadController(linkingSessionWithAttachments)
    val request =
      fakeRequest.withHeaders(HOST -> "localhost:9523").withBody(Json.obj("evidenceType" -> evidenceType))
    when(mockBusinessRatesChallengeService.persistSessionData(any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    val result = uploadController.continue(evidenceChoice)(request)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }
}
