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
import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import connectors.{CapacityDeclaration, EnvelopeConnector}
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matches}
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
  val fileUploadConnectorStub = new FileUploadConnector(wsClient, envConnectorStub) {
    override def createEnvelope(metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[String] = {
      Future.successful("some string")
    }
    override def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier): Future[Unit] = {
      Future.successful(())
    }
  }


  object TestUploadRatesBill extends UploadRatesBill(fileUploadConnectorStub, envConnectorStub)  {
    val property = arbitrary[Property].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get

    override lazy val propertyLinks = StubPropertyLinkConnector
    val submissionId = shortString.sample.get
    val personId = Math.abs(arbitrary[Long].sample.get)
    val envelopeId = "12345"
    val session = LinkingSession(property.address, property.uarn, envelopeId, submissionId, personId, Some(CapacityDeclaration(Owner, true, None, true, None)))

    override lazy val withLinkingSession = new StubWithLinkingSession(session,
      person, arbitrary[GroupAccount].sample.get)
    override lazy val linkingSession = new StubLinkingSessionRepository
  }

  "Upload Rates Bill upload page" must "contain a file input" in  {
    val res = TestUploadRatesBill.show()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainFileInput("ratesBill")
  }

  it must "redirect to the rates-bill-submitted page if a valid rates bill is uploaded" in {
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
    redirectLocation(res).get must include (routes.UploadRatesBill.fileUploaded().url)
  }

  it must "show an error if the user doesn't upload any file" in {
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

  "The upload rates bill success page" must "indicate that the request has been submitted" in {
    val res = TestUploadRatesBill.fileUploaded()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))

    page.mustContainSuccessSummary(s"We’ve received your request to add ${
      TestUploadRatesBill.property.address} to your business’s customer record" +
      s" Your submission ID is ${TestUploadRatesBill.submissionId}")
  }

  it must "contain a link to the dashboard" in {
    val request = FakeRequest().withSession(token)
    val res = TestUploadRatesBill.fileUploaded()(request)
    status(res) mustBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(res)))
    page.mustContainLink("#backToDashBoard", routes.Dashboard.manageProperties.url)
  }
}
