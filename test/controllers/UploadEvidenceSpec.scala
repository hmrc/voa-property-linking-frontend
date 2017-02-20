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

import connectors.CapacityDeclaration
import connectors.fileUpload.FileUploadConnector
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import resources._
import utils.{FileUploadTestHelpers, HtmlPage, StubPropertyLinkConnector, StubWithLinkingSession}

import scala.concurrent.Future


class UploadEvidenceSpec extends ControllerSpec with FileUploadTestHelpers {
  implicit val request = FakeRequest().withSession(token)

  val mockFileUploads = mock[FileUploadConnector]
  when(mockFileUploads.uploadFile(anyString(), anyString(), anyString(), any())(any())).thenReturn(Future.successful(()))

  object TestUploadEvidence extends UploadEvidence(mockFileUploads)  {
    override val withLinkingSession = new StubWithLinkingSession(arbitrary[Property].sample.get, arbitrary[CapacityDeclaration].sample.get,
      arbitrary[DetailedIndividualAccount].sample.get, arbitrary[GroupAccount].sample.get)
    override val propertyLinks = StubPropertyLinkConnector
  }

  "Upload Evidence page" must "contain a file input" in {
    val res = TestUploadEvidence.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("evidence")
  }

  it must "redirect to the evidence-submitted page if valid evidence has been uploaded" in {
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
    header("location", res).get.contains(routes.UploadEvidence.fileUploaded.url) mustBe true
  }

  it must "show an error if the user does not upload any evidence" in {
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

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property.", "Please upload some evidence."))
    page.mustContainFieldErrors(("evidence_", "Please upload some evidence."))
  }

  it must "show an error if the user uploads a file greater than 10MB" in {
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

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property.", "File size must be less than 10MB"))
    page.mustContainFieldErrors(("evidence_", "File size must be less than 10MB"))
  }

  it must "show an error if the user uploads a file that is not a JPG or PDF" in {
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

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property.", "File must be a PDF or JPG"))
    page.mustContainFieldErrors(("evidence_", "File must be a PDF or JPG"))
  }
}
