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

import java.io.File

import config.{ApplicationConfig, VPLHttp}
import connectors.EnvelopeConnector
import connectors.fileUpload.{FileMetadata, FileUploadConnector}
import controllers.VoaPropertyLinkingSpec
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import play.api.test.Helpers._
import services.BusinessRatesAttachmentService
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UnhealthyServiceException}
import utils._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class UploadRatesBillSpec extends VoaPropertyLinkingSpec with FileUploadTestHelpers {

  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentService]


  "Upload Rates Bill upload page" must "contain a file input" in {
    val html = HtmlPage(uploadRatesBillPage)
    html.mustContainFileInput("ratesBill")
  }

  it must "contain a hidden input for the evidence type" in {
    val html = uploadRatesBillPage
    val csrfToken: Element => Boolean = _.attr("name") == "csrfToken"

    val evidenceType = html.select("input[type=hidden][name=evidenceType]").asScala.filterNot(csrfToken).head
    evidenceType.`val`() mustBe "ratesBill"
  }
  it must "display an error if the user uploads a file over 10MB" in {
    val linkingSession = arbitrary[LinkingSession]
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestUploadRatesBill.show()(request)
    status(res) mustBe OK
    val html = Jsoup.parse(contentAsString(res))
    html.select()
  }

  it must "display an error if the user uploads a file which is not a PDF or JPEG" in {
    val linkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestUploadRatesBill.show(errorCode = Some(UNSUPPORTED_MEDIA_TYPE), None)(request)
    status(res) mustBe UNSUPPORTED_MEDIA_TYPE

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=ratesBill] span.error-message").text mustBe "File must be a PDF or JPG"
  }

  it must "display a service unavailable page when the file upload service is not available" in {
    val testController = new UploadEvidence( withLinkingSession, brokenCircuit, mockFileUploadConnector)

    val linkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = testController.show(None, None)(request)
    status(res) mustBe SERVICE_UNAVAILABLE

    val html = Jsoup.parse(contentAsString(res))
    html.select("h1.heading-xlarge").text mustBe "Service unavailable"
  }

  lazy val uploadRatesBillPage = {
    val linkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])
    val res = TestUploadRatesBill.show(errorCode = None, errorMessage = None)(request)
    status(res) mustBe OK
    Jsoup.parse(contentAsString(res))
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
