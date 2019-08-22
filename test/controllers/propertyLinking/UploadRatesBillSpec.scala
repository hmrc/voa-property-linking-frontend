/*
 * Copyright 2019 HM Revenue & Customs
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

import config.VPLHttp
import controllers.VoaPropertyLinkingSpec
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeRequest
import repositories.SessionRepo
import resources._
import controllers.propertyLinking.routes._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import services.BusinessRatesAttachmentService
import utils._
import akka.stream.Materializer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class UploadRatesBillSpec extends VoaPropertyLinkingSpec with FakeObjects{
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
  implicit def materializer: Materializer = app.injector.instanceOf[Materializer]

  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentService]
  def controller() =
    new UploadRatesBill(preAuthenticatedActionBuilders(), withLinkingSession, mockBusinessRatesAttachmentService)


  it should  "return file upload initiate success" in {
      val linkingSession = arbitrary[LinkingSession]
      withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
      val request = FakeRequest(POST, "").withBody(
        Json.obj(
          "fileName" -> "test.jpg",
          "mimeType" -> "image/jpeg"))

      when(mockBusinessRatesAttachmentService.initiateAttachmentUpload(any())(any(), any[HeaderCarrier])).thenReturn(Future.successful(preparedUpload))
      var result = controller().initiate()(request)
      status(result) mustBe OK
    }

  it should "return remove file success" in {
      val linkingSession = arbitrary[LinkingSession]
      withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
      val request = FakeRequest(POST, "").withBody()
      when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful())
      var result = controller().removeFile("01222333")(request).run()
      status(result) mustBe OK
    }

  it should  "show error if no files selected" in {
      val linkingSession = arbitrary[LinkingSession]
      withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
      val request = FakeRequest(POST, "").withBody()
      val postRequest = request.withFormUrlEncodedBody()

      val result = controller().continue()(postRequest)

      status(result) mustBe BAD_REQUEST
    }

  it should  "show next Page if files selected" in {
      val linkingSession = arbitrary[LinkingSession].copy(uploadEvidenceData = uploadEvidenceData)
      withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
     when(mockBusinessRatesAttachmentService.persistSessionData(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful())

    val request = FakeRequest(POST, "").withBody()
      val postRequest = request.withFormUrlEncodedBody()
      val result = controller().continue()(postRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.Declaration.show().url)
    }


  lazy val uploadRatesBillPage = {
    val linkingSession = arbitrary[LinkingSession]
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
    val res = TestUploadRatesBill.show()(request)
    status(res) mustBe OK
    val html = Jsoup.parse(contentAsString(res))
    html.select("h1.heading-xlarge").text mustBe "Submit a copy of your business rates bill"
  }

  implicit lazy val request = FakeRequest().withSession(token).withHeaders(HOST -> "localhost:9523")

  implicit lazy val hc = HeaderCarrier()
  lazy val wsHttp = app.injector.instanceOf[VPLHttp]

  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  object TestUploadRatesBill extends UploadRatesBill(preAuthenticatedActionBuilders(), withLinkingSession, mockBusinessRatesAttachmentService)

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

}
