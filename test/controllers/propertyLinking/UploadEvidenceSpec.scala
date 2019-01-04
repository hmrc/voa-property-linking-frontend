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
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UnhealthyServiceException}
import utils._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class UploadEvidenceSpec extends VoaPropertyLinkingSpec with FileUploadTestHelpers {

  override val additionalAppConfig = Seq("featureFlags.fileUploadEnabled" -> "true")

  val fileMetadata = FileMetadata(Some("OTHER"), Some(Lease))
  lazy val mockFileUploadConnector = mock[FileUploadConnector]
  when(mockFileUploadConnector.getFileMetadata(any())(any())).thenReturn(Future.successful(fileMetadata))

  "Upload Evidence page" must "contain a file input" in {
    val page = HtmlPage(uploadEvidencePage)
    page.mustContainFileInput("evidence")
  }

  it must "contain a file type dropdown" in {
    val html = uploadEvidencePage
    val options = html.select("select option").asScala.map(_.text)
    options must contain theSameElementsAs EvidenceType.all.map { et => Messages(et.msgKey) }
  }

  it must "submit to the file upload service, with valid success and failure callback URLs" in {
    val html = uploadEvidencePage
    val successUrl = routes.UploadEvidence.fileUploaded().absoluteURL()
    val failureUrl = routes.UploadEvidence.show().absoluteURL()

    val formAction = html.select("form").attr("action")
    formAction must fullyMatch regex s"http://localhost:8899/file-upload/upload/envelopes/$envelopeId/files/(.*?)?redirect-success-url=$successUrl&redirect-error-url=$failureUrl"
  }

  it must "display an error if the user uploads a file over 10MB" in {
    val linkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestUploadEvidence.show(errorCode = Some(REQUEST_ENTITY_TOO_LARGE), None)(request)
    status(res) mustBe REQUEST_ENTITY_TOO_LARGE

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=evidence] span.error-message").text mustBe "File size must be less than 10MB"
  }

  it must "display an error if the user uploads a file which is not a PDF or JPEG" in {
    val linkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestUploadEvidence.show(errorCode = Some(UNSUPPORTED_MEDIA_TYPE), None)(request)
    status(res) mustBe UNSUPPORTED_MEDIA_TYPE

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=evidence] span.error-message").text mustBe "File must be a PDF or JPG"
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

  lazy val uploadEvidencePage = {
    val linkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestUploadEvidence.show(errorCode = None, errorMessage = None)(request)
    status(res) mustBe OK
    Jsoup.parse(contentAsString(res))
  }

  implicit lazy val request = FakeRequest().withSession(token).withHeaders(HOST -> "localhost:9523")

  lazy val wsClient = app.injector.instanceOf[VPLHttp]

  lazy val envConnectorStub = new EnvelopeConnector(StubServicesConfig, wsClient) {
    override def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] =  {
      Future.successful(envelopeId)
    }
    override def storeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] =  {
      Future.successful(envelopeId)
    }
  }

  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  object TestUploadEvidence extends UploadEvidence( withLinkingSession, unbreakableCircuit, mockFileUploadConnector)

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  lazy val envelopeId: String = shortString

  implicit lazy val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  lazy val unbreakableCircuit = new FileUploadCircuitBreaker(mock[CircuitBreakerConfig], mock[FileUploadConnector]) {
    override def apply[T](f: => Future[T])(implicit hc: HeaderCarrier): Future[T] = f
  }

  lazy val brokenCircuit = new FileUploadCircuitBreaker(mock[CircuitBreakerConfig], mock[FileUploadConnector]) {
    override def apply[T](f: => Future[T])(implicit hc: HeaderCarrier) = Future.failed(new UnhealthyServiceException("file upload isn't feeling well"))
  }
}
