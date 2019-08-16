package controllers.propertyLinking

import controllers.VoaPropertyLinkingSpec
import models.upscan._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.HttpVerbs.{GET => _, POST => _}
import play.api.libs.json.{JsString, Json}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import services.BusinessRatesAttachmentService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{FakeObjects, StubWithLinkingSession}

import scala.concurrent.Future

class UploadRatesBillSpec extends VoaPropertyLinkingSpec with FakeObjects{

  private val FILE_REFERENCE: String = "1862956069192540"
  private val cacheId = "id"
  val REASON_CODE = "41a"
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)
  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentService]
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  def controller() =
    new UploadRatesBill(preAuthenticatedActionBuilders(), withLinkingSession, mockBusinessRatesAttachmentService)

  "SupportDocumentsController" must {

    "return OK and the correct view for GET" in {
      val result: Future[Result] = controller().show()(fakeRequest)

      status(result) mustBe OK
      val viewContent = contentAsString(result)
      contentAsString(result) mustBe viewAsString()(fakeRequest)
    }

    "return file upload initiate success" in {

      val request = FakeRequest(POST, "").withBody(
        Json.obj(
          "fileName"     -> "test.jpg",
          "mimeType"     -> "image/jpeg"))

      when(mockBusinessRatesChallengeService.initiateAttachmentUpload(any(), any(), any(), any())(any(), any[HeaderCarrier])).thenReturn(Future.successful(preparedUpload))
      var result = controller(getCacheMapWithData(Map("submissionId" ->  JsString("11111")))).initiate(NormalMode)(request)
      status(result) mustBe OK
    }

    "return remove file success" in {

      val request = FakeRequest(POST, "").withBody()
      val cacheMap = CacheMap("100", Map("121221" -> Json.toJson(WhatIsWrongWithValuationStep(2, "details_wrong"))))

      when(mockBusinessRatesChallengeService.saveSupportDocumentsStep(any(), any())).thenReturn(Future.successful(cacheMap))

      var result = controller(getCacheMapWithData(Map("submissionId" ->  JsString("11111")))).removeFile("01222333", NormalMode)(request).run()
      status(result) mustBe OK
    }

    "show error if no files selected" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody()

      val result = controller(getCacheMapWithData(Map("submissionId" ->  JsString("11111")))).nextScreen(NormalMode)(postRequest)

      status(result) mustBe BAD_REQUEST
    }

    "show next Page if files selected" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody()
      dataCacheConnector.addData(Map("submissionId" ->  JsString("11111"), "SupportDocumentsId" ->  Json.toJson(supportDocumentsStep)))
      val result = controller(getCacheMapWithData(Map("submissionId" ->  JsString("11111")))).nextScreen(NormalMode)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }

    "redirect to Session Expired for a GET if no existing data is found" in {
      val result = controller(dontGetAnyData).onPageLoad(NormalMode)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad().url)
    }

  }
}
