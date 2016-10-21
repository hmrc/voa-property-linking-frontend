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

import auth.GGAction
import connectors.propertyLinking.PropertyLinkConnector
import connectors.{CapacityDeclaration, VPLAuthConnector}
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MultipartFormData, Result}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import utils._
import utils.MultipartFormDataWritable.anyContentAsMultipartFormWritable

import scala.concurrent.Future

class RatesBillUploadSpec extends ControllerSpec {
  implicit val request = FakeRequest().withSession(token)

  import TestData._

  object TestUploadRatesBill extends UploadRatesBill {
    override lazy val withLinkingSession = new StubWithLinkingSession(property, capacityDeclaration)
    override lazy val fileUploadConnector = StubFileUploadConnector
    override lazy val propertyLinkConnector  = new StubPropertyLinkConnector
  }

  "Upload Rates Bill upload page" must "allow the user to upload some evidence or not" in {
    val res = TestUploadRatesBill.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainRadioSelect("hasRatesBill", Seq("doeshaveratesbill", "doesnothaveratesbill"))
  }

  it must "redirect to the rates-bill-submitted page if a bill has been uploaded" in {
    val testFile = getClass.getResource("/fileUpload.txt").getPath
    val path = testFile + ".tmp"
    val tmpFile = TemporaryFile(new File(path))
    Files.copy(Paths.get(testFile), Paths.get(path))
    //val file = new File(path)
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill2")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map("hasRatesBill" -> Seq(DoesHaveRatesBill.name)),
          files = Seq(
            FilePart("ratesBill", path, None, tmpFile)
          ),
          badParts = Seq.empty,
          missingFileParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadRatesBill.submit()(req)
    tmpFile.clean()
    status(res) mustBe SEE_OTHER
    header("location", res).get mustEqual "/property-linking/rates-bill-submitted"
  }

  it must "show an error if the user says he wants to submit a rates bill but doesn't" in {
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-rates-bill2")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map("hasRatesBill" -> Seq(DoesHaveRatesBill.name)),
          files = Seq(), //We're not submitting any file here.
          badParts = Seq.empty,
          missingFileParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadRatesBill.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("ratesBill", "Please upload a copy of the rates bill", "please select a rates bill"))
    page.mustContainFieldErrors(("ratesBill", "please select a rates bill"))
  }

  //"When they specify they have a rates bill" - {
  //  HTTP.stubRatesBillCheck(baRef, validRatesBill, ratesBillAccepted = true)
  //  HTTP.stubFileUpload(groupId, sid, "ratesBill", ("ratesbill.pdf", validRatesBill))
  //  val result = Page.postValid("/property-linking/upload-rates-bill", "hasRatesBill" -> "doeshaveratesbill")

  //  "Their link request is submitted" in {
  //    HTTP.verifyPropertyLinkRequest(uarn, groupId, expectedLink)
  //  }

  //  "They are sent to the rates bill accepted page" in {
  //    result.header.headers.get("location").value mustEqual "/property-linking/rates-bill-submitted"
  //  }
  //}

  object TestData {
    lazy val baRef = "sfku03802342"
    lazy val uarn = "uarn03802342"
    lazy val address = Address("leen1", "leen2", "leen3", "AA11 1AA")
    lazy val property = Property(uarn, baRef, address, false, true)
    lazy val capacityDeclaration =  CapacityDeclaration(OwnerLandlord,  DateTime.now(), None)
  }
}
