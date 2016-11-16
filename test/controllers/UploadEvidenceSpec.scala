/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.fileUpload.FileUpload
import connectors.CapacityDeclaration
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.mvc.MultipartFormData
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import utils.{HtmlPage, StubPropertyLinkConnector, StubWithLinkingSession}
import org.scalatest.mock.MockitoSugar
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart



class UploadEvidenceSpec extends ControllerSpec with MockitoSugar {
  implicit val request = FakeRequest().withSession(token)
  import TestData._

  val mockFileUploads = mock[FileUpload]
  object TestUploadEvidence extends UploadEvidence(mockFileUploads)  {
    override lazy val withLinkingSession = new StubWithLinkingSession(property, capacityDeclaration)
    override lazy val propertyLinkConnector  = new StubPropertyLinkConnector
  }

  "Upload Evidence page" must "allow the user to upload some evidence or not" in {
    val res = TestUploadEvidence.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("evidence_")
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
    header("location", res).get.contains("/property-linking/evidence-uploaded") mustBe true
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

  object TestData {
    lazy val uarn = 987654
    lazy val baRef = "baRef-asdfjlj23l4j23"
    lazy val address = Address("", "", "", "AA11 1AA")
    lazy val property = Property(uarn, baRef, address, false, "SCAT", "description", "B")
    lazy val capacityDeclaration = CapacityDeclaration(Owner, DateTime.now, None)
  }
}
