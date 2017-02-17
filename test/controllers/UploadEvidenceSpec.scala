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
import java.nio.file.{Files, Paths}

import connectors.CapacityDeclaration
import connectors.fileUpload.FileUpload
import models._
import org.jsoup.Jsoup
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import utils.{HtmlPage, StubPropertyLinkConnector, StubWithLinkingSession}
import resources._


class UploadEvidenceSpec extends ControllerSpec with MockitoSugar {
  implicit val request = FakeRequest().withSession(token)

  val mockFileUploads = mock[FileUpload]
  object TestUploadEvidence extends UploadEvidence(mockFileUploads)  {
    override lazy val withLinkingSession = new StubWithLinkingSession(arbitrary[Property].sample.get, arbitrary[CapacityDeclaration].sample.get,
      arbitrary[DetailedIndividualAccount].sample.get, arbitrary[GroupAccount].sample.get)
    override lazy val propertyLinkConnector = StubPropertyLinkConnector
  }

  "Upload Evidence page" must "allow the user to upload some evidence or not" in {
    val res = TestUploadEvidence.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("evidence")
  }

  it must "redirect to the evidence-submitted page if some evidence has been uploaded" in {
    val testFile = getClass.getResource("/fileUpload.txt").getPath
    val path = testFile + ".tmp"
    val tmpFile = TemporaryFile(new File(path))
    Files.copy(Paths.get(testFile), Paths.get(path))
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(
            "hasEvidence" -> Seq(DoesHaveEvidence.name),
            "evidenceType" -> Seq(OtherUtilityBill.name)
          ),
          files = Seq(
            FilePart("evidence[]", path, None, tmpFile)
          ),
          badParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadEvidence.submit()(req)
    tmpFile.clean()
    status(res) mustBe SEE_OTHER
    header("location", res).get.contains(routes.UploadEvidence.evidenceUploaded.url) mustBe true
  }

  it must "show an error if the user says he wants to submit further evidence but doesn't" in {
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map(
            "hasEvidence" -> Seq(DoesHaveEvidence.name),
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

}
