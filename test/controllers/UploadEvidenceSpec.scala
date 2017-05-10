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

import connectors.EnvelopeConnector
import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.SessionRepo
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UploadEvidenceSpec extends ControllerSpec with FileUploadTestHelpers {
  implicit val request = FakeRequest().withSession(token)

  val wsClient = app.injector.instanceOf[WSClient]
  val envConnectorStub = new EnvelopeConnector(wsClient) {
    override def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] =  {
      Future.successful(envelopeId)
    }
    override def storeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] =  {
      Future.successful(envelopeId)
    }
  }

  val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)
  object TestUploadEvidence extends UploadEvidence(mockFileUploads, envConnectorStub, mockSessionRepo, withLinkingSession)  {
    override val propertyLinks = StubPropertyLinkConnector
  }

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }

  lazy val mockFileUploads = {
    val m = mock[FileUploadConnector]
    when(m.createEnvelope(any[EnvelopeMetadata])(any[HeaderCarrier])).thenReturn(Future.successful(envelopeId))
    when(m.uploadFile(matching(envelopeId), anyString, anyString, any[File])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    m
  }

  lazy val envelopeId: String = shortString

  "Upload Evidence page" must "contain a file input" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestUploadEvidence.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("evidence")
  }

  it must "redirect to the evidence-submitted page if valid evidence has been uploaded" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession].copy(envelopeId = envelopeId), arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(
            "evidenceType" -> Seq(OtherUtilityBill.name)
          ),
          files = Seq(
            FilePart("evidence[]", validFilePath, Some(validMimeType), validFile)
          ),
          badParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadEvidence.submit()(req)

    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(propertyLinking.routes.Declaration.show.url)

    verify(mockFileUploads).uploadFile(
      matching(envelopeId),
      matching(validFilePath),
      matching(validMimeType),
      any())(any()
    )
  }

  it must "show an error if the user does not upload any evidence" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(
            "evidenceType" -> Seq(OtherUtilityBill.name)
          ),
          files = Seq(),
          badParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadEvidence.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property", "Please upload some evidence"))
    page.mustContainFieldErrors(("evidence_", "Please upload some evidence"))
  }

  it must "show an error if the user uploads a file greater than 10MB" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(
            "evidenceType" -> Seq(OtherUtilityBill.name)
          ),
          files = Seq(
            FilePart("evidence[]", largeFilePath, Some(validMimeType), largeFile)
          ),
          badParts = Nil
        )
      )

    val res = TestUploadEvidence.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property", "File size must be less than 10MB"))
    page.mustContainFieldErrors(("evidence_", "File size must be less than 10MB"))
  }

  it must "show an error if the user uploads a file that is not a JPG or PDF" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(
            "evidenceType" -> Seq(OtherUtilityBill.name)
          ),
          files = Seq(
            FilePart("evidence[]", unsupportedFilePath, Some(unsupportedMimeType), unsupportedFile)
          ),
          badParts = Nil
        )
      )

    val res = TestUploadEvidence.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property", "File must be a PDF or JPG"))
    page.mustContainFieldErrors(("evidence_", "File must be a PDF or JPG"))
  }
}
