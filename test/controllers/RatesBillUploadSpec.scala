/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import java.io.File

import _root_.session.LinkingSession
import connectors.EnvelopeConnector
import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RatesBillUploadSpec extends ControllerSpec with FileUploadTestHelpers {
  implicit val request = FakeRequest().withSession(token)

  implicit val hc = HeaderCarrier()
  val wsClient = app.injector.instanceOf[WSClient]

  val envConnectorStub = new EnvelopeConnector(wsClient) {
    override def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] =  {
      Future.successful(envelopeId)
    }
    override def storeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] =  {
      Future.successful(envelopeId)
    }
  }

  object TestUploadRatesBill extends UploadRatesBill(mockFileUploads, envConnectorStub)  {
    val property = arbitrary[Property].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get

    override lazy val propertyLinks = StubPropertyLinkConnector

    override lazy val withLinkingSession = StubWithLinkingSession

    override lazy val linkingSession = new StubLinkingSessionRepository
  }
  
  lazy val mockFileUploads = {
    val m = mock[FileUploadConnector]
    when(m.createEnvelope(any[EnvelopeMetadata])(any[HeaderCarrier])).thenReturn(Future.successful(envelopeId))
    when(m.uploadFile(matching(envelopeId), anyString, anyString, any[File])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    m
  }

  lazy val envelopeId: String = shortString

  "Upload Rates Bill upload page" must "contain a file input" in  {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
    val res = TestUploadRatesBill.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("ratesBill")
  }

  it must "redirect to the declaration page if a valid rates bill is uploaded" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession].copy(envelopeId = envelopeId), arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(),
          files = Seq(
            FilePart("ratesBill[]", validFilePath, Some(validMimeType), validFile)
          ),
          badParts = Seq.empty
        )
      ).withSession(token)

    val res = TestUploadRatesBill.submit()(req)

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(propertyLinking.routes.Declaration.show.url)

    verify(mockFileUploads).uploadFile(
        matching(envelopeId),
        matching(validFilePath),
        matching(validMimeType),
        any())(any())
  }

  it must "show an error if the user doesn't upload any file" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(),
          files = Seq(), //We're not submitting any file here.
          badParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadRatesBill.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("ratesBill", "Please upload a copy of the rates bill", "Please select a rates bill"))
    page.mustContainFieldErrors(("ratesBill_", "Please select a rates bill"))
  }

  it must "show an error if the uploaded file is too large" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(),
          files = Seq(
            FilePart("ratesBill[]", largeFilePath, None, largeFile)
          ),
          badParts = Nil
        )
      )

    val res = TestUploadRatesBill.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("ratesBill", "Please upload a copy of the rates bill", "File size must be less than 10MB"))
    page.mustContainFieldErrors(("ratesBill_", "File size must be less than 10MB"))
  }

  it must "show an error if the user uploads a file that is not a JPG or PDF" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(),
          files = Seq(
            FilePart("ratesBill[]", unsupportedFilePath, Some(unsupportedMimeType), unsupportedFile)
          ),
          badParts = Nil
        )
      )

    val res = TestUploadRatesBill.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("ratesBill", "Please upload a copy of the rates bill", "File must be a PDF or JPG"))
    page.mustContainFieldErrors(("ratesBill_", "File must be a PDF or JPG"))
  }
}
