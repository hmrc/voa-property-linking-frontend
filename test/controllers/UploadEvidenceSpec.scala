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

import connectors.{CapacityDeclaration, OtherEvidenceFlag, PropertyLink}
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.mvc.MultipartFormData
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import utils.{HtmlPage, StubWithLinkingSession}


class UploadEvidenceSpec extends ControllerSpec {
  implicit val request = FakeRequest().withSession(token)
  import TestData._

  object TestUploadEvidence extends UploadEvidence {
    override lazy val withLinkingSession = new StubWithLinkingSession(property, capacityDeclaration)
  }

  "Upload Evidence page" must "allow the user to upload some evidence or not" in {
    val res = TestUploadEvidence.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainRadioSelect("hasevidence", Seq("doeshaveevidence", "doesnothaveevidence"))
    page.mustContainMultiFileInput("evidence")
  }

  it must "show an error if the user says he wants to submit further evidence but doesn't" in {
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map("hasEvidence" -> Seq(DoesHaveEvidence.name)),
          files = Seq(),
          badParts = Seq.empty,
          missingFileParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadEvidence.submit()(req)
    status(res) mustBe BAD_REQUEST
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSummaryErrors(("evidence", "Please upload evidence so that we can verify your link to the property.", "Please upload some evidence."))
    page.mustContainFieldErrors(("evidence", "Please upload some evidence."))
  }
  it should "redirect the user to the no evidence upload page if the user doesn't upload any evidence" ignore {
    val req = FakeRequest(Helpers.POST, "/property-linking/upload-evidence")
      .withMultipartFormDataBody(
        MultipartFormData(
          dataParts = Map("hasEvidence" -> Seq(DoesNotHaveEvidence.name)), 
          files = Seq(),
          badParts = Seq.empty,
          missingFileParts = Seq.empty
        )
      ).withSession(token)
    val res = TestUploadEvidence.submit()(req)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

  }
  //"Given an interested person is being asked to provide additional evidence" - {
  //  implicit val sid: SessionId = java.util.UUID.randomUUID.toString
  //  implicit val session = GGSession(userId, token)
  //  HTTP.stubKeystoreSession(SessionDocument(property, envelopeId, Some(declaration)), Seq(Account(userId, false)))
  //  HTTP.stubAuthentication(session)
  //  HTTP.stubGroupId(session, groupId)

  //  "When they arrive at the upload evidence page" - {
  //    val page = Page.get("/property-linking/upload-evidence")

  //    "They are asked if they have further evidence" ignore {
  //      page.mustContainRadioSelect("hasEvidence", Seq("doeshaveevidence", "doesnothaveevidence"))
  //    }

  //    "They are able to upload files as evidence" ignore {
  //      page.mustContainMultiFileInput("evidence")
  //    }
  //  }

  //  "When they specify they have evidence and upload upto 3 files" - {
  //    HTTP.stubFileUpload(groupId, sid, "evidence", ("file1.pdf", bytes1), ("file2.pdf", bytes2))
  //    val result = Page.postValid("/property-linking/upload-evidence", "hasEvidence" -> "doeshaveevidence")

  //    "Their link request is submitted" ignore {
  //      HTTP.verifyPropertyLinkRequest(uarn, groupId, expectedLink)
  //    }

  //    "They are taken to the evidence uploaded confirmation page" ignore {
  //      result.header.headers("location") mustEqual "/property-linking/evidence-uploaded"
  //    }
  //  }

  //  "But if they specify they do not have any evidence" - {
  //    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
  //    HTTP.stubKeystoreSession(SessionDocument(property, envelopeId, Some(declaration)), Seq(Account(userId, false)))
  //    val result = Page.postValid("/property-linking/upload-evidence", "hasEvidence" -> "doesnothaveevidence")

  //    "Their link request is submitted" ignore {
  //      HTTP.verifyPropertyLinkRequest(uarn, groupId, expectedLink)
  //    }

  //    "They are taken to the no evidence provided page" ignore {
  //      result.header.headers("location") mustEqual "/property-linking/no-evidence-uploaded"
  //    }
  //  }

  //  "When they do not supply a valid response" - {
  //    implicit val sid: SessionId = java.util.UUID.randomUUID.toString
  //    HTTP.stubKeystoreSession(SessionDocument(property, envelopeId, Some(declaration)), Seq(Account(userId, false)))
  //    HTTP.stubFileUpload(groupId, sid, "evidence", ("1.pdf", bytes1), ("2.pdf", bytes1), ("3.pdf", bytes1), ("4.pdf", bytes1))
  //    val page = Page.postInvalid("/property-linking/upload-evidence", "hasEvidence" -> "doeshaveevidence")

  //    "An error summary is shown" ignore {
  //      page.mustContainSummaryErrors(
  //        ("evidence", "Please upload evidence so that we can verify your link to the property.", "Only 3 files may be uploaded")
  //      )
  //    }

  //    "A field-level error is shown for each invalid field" ignore {
  //      page.mustContainFieldErrors("evidence" -> "Only 3 files may be uploaded")
  //    }
  //  }
  //}

  object TestData {
    lazy val uarn = "uarn4"
    lazy val envelopeId = "asdfasfsaf"
    lazy val baRef = "baRef-asdfjlj23l4j23"
    lazy val userId = "sdfksjdlf34233gr6"
    lazy val groupId = "9qiouasg099awg"
    lazy val token = "jaslasknal;;"
    lazy val address = Address("", "", "", "AA11 1AA")
    lazy val property = Property(uarn, baRef, address, false, false)
    lazy val capacityDeclaration = CapacityDeclaration(OwnerLandlord, DateTime.now, None)
    lazy val bytes1 = (44 to 233).map(_.toByte).toArray
    lazy val bytes2 = (200 to 433).map(_.toByte).toArray
    lazy val expectedLink = PropertyLink(uarn, groupId, capacityDeclaration, DateTime.now, Seq(2017), true, OtherEvidenceFlag)

  }
}
