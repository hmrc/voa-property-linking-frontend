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

import connectors.CapacityDeclaration
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.MultipartFormData
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import _root_.session.LinkingSession
import config.VPLSessionCache
import connectors.fileUpload.FileUpload
import org.scalatest.mock.MockitoSugar
import utils._


class RatesBillUploadSpec extends ControllerSpec with MockitoSugar{
  implicit val request = FakeRequest().withSession(token)

  import TestData._

  val mockFileUploads = mock[FileUpload]
  object TestUploadRatesBill extends UploadRatesBill(mockFileUploads)  {
    override lazy val withLinkingSession = new StubWithLinkingSession(property, capacityDeclaration)
    override lazy val propertyLinkConnector = StubPropertyLinkConnector
    lazy val sessionCache = new VPLSessionCache(StubHttp)
    override lazy val sessionRepository = new StubLinkingSessionRepository(session, sessionCache)
  }

  "Upload Rates Bill upload page" must "allow the user to upload some evidence or not" in {
    val res = TestUploadRatesBill.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("ratesBill_")
  }

  it must "redirect to the rates-bill-submitted page if a bill has been uploaded" in {
    val testFile = getClass.getResource("/fileUpload.txt").getPath
    val path = testFile + ".tmp"
    val tmpFile = TemporaryFile(new File(path))
    Files.copy(Paths.get(testFile), Paths.get(path))
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map("hasRatesBill" -> Seq(DoesHaveRatesBill.name)),
          files = Seq(
            FilePart("ratesBill[]", path, None, tmpFile)
          ),
          badParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadRatesBill.submit()(req)
    tmpFile.clean()
    status(res) mustBe SEE_OTHER
    header("location", res).get.contains("/property-linking/rates-bill-submitted") mustBe true
  }

  it must "show an error if the user says he wants to submit a rates bill but doesn't" in {
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map("hasRatesBill" -> Seq(DoesHaveRatesBill.name)),
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

  it must "indicate that the request has been submitted" in {
    val res = TestUploadRatesBill.ratesBillUploaded()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainSuccessSummary("✔ Property linking request submitted")
  }

  it must "contains a link to the dashboard" in {
    val request = FakeRequest().withSession(token)
    val res = TestUploadRatesBill.ratesBillUploaded()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainLink("#backToDashBoard", "/property-linking/manage-properties")
  }

  object TestData {
    lazy val baRef = "sfku03802342"
    lazy val uarn = 2345678
    lazy val address = Address("leen1", "leen2", "leen3", "AA11 1AA")
    lazy val property = Property(uarn, baRef, address, false, "SCAT", "description", "C")
    lazy val capacityDeclaration =  CapacityDeclaration(Owner,  DateTime.now(), None)
    lazy val session = LinkingSession(property, "envelopeId", "submissionId")
  }
}
