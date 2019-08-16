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

import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import controllers.VoaPropertyLinkingSpec
import models._
import models.upscan.UploadedFileDetails
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import session.LinkingSessionRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import services.BusinessRatesAttachmentService
import utils.{FakeObjects, HtmlPage, StubWithLinkingSession}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class DeclarationSpec extends VoaPropertyLinkingSpec with MockitoSugar with FakeObjects{

  override val additionalAppConfig = Seq("featureFlags.fileUploadEnabled" -> "true")

  "The declaration page" should "include a checkbox to allow the user to accept the declaration" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.show()(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.select("input[type=checkbox]").asScala.map(_.id) must contain ("declaration")
  }

  it should "require the user to accept the declaration to continue" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest())
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainSummaryErrors(("declaration", "Declaration", "You must agree to the declaration to continue"))
  }

  it should "submit the property link if the user accepts the declaration" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession]
    when(mockBusinessRatesAttachmentService.submitFiles(any[String], any[Option[Map[String, UploadedFileDetails]]])(any[LinkingSessionRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(List(Some(attachment))))
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Declaration.confirmation().url)

    verify(mockPropertyLinkConnector, times(1)).createPropertyLink()(any[LinkingSessionRequest[_]])
  }

  it should "display the normal confirmation page when the user has uploaded a rates bill" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession]

    when(mockBusinessRatesAttachmentService.submitFiles(any[String], any[Option[Map[String, UploadedFileDetails]]])(any[LinkingSessionRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(List(Some(attachment))))
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Declaration.confirmation().url)

    val confirmation = TestDeclaration.confirmation()(FakeRequest())
    status(confirmation) mustBe OK

    val html = Jsoup.parse(contentAsString(confirmation))
    html.title mustBe s"We’ve received your request to add the property to your business’s customer record"
    html.body().text must include (linkingSession.submissionId)
  }

  it should "display the normal confirmation page when the user has uploaded other evidence" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession]

    when(mockBusinessRatesAttachmentService.submitFiles(any[String], any[Option[Map[String, UploadedFileDetails]]])(any[LinkingSessionRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(List(Some(attachment))))
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Declaration.confirmation().url)

    val confirmation = TestDeclaration.confirmation()(FakeRequest())
    status(confirmation) mustBe OK

    val html = Jsoup.parse(contentAsString(confirmation))
    html.title mustBe s"We’ve received your request to add the property to your business’s customer record"
    html.body().text must include (linkingSession.submissionId)
  }

  it should "display the no evidence confirmation page when the user has not uploaded any evidence" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession]
    when(mockBusinessRatesAttachmentService.submitFiles(any[String], any[Option[Map[String, UploadedFileDetails]]])(any[LinkingSessionRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(List(Some(attachment))))
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Declaration.noEvidence().url)

    val confirmation = TestDeclaration.noEvidence()(FakeRequest())
    status(confirmation) mustBe OK

    val html = Jsoup.parse(contentAsString(confirmation))
    html.title mustBe "We’re sorry, but you can’t proceed with this form."
    html.body().text must include (linkingSession.submissionId)
  }

  "The confirmation page" should "display the submission ID" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession]
    withLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.confirmation()(FakeRequest())
    status(res) mustBe OK

    contentAsString(res) must include (linkingSession.submissionId)
  }

  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  private object TestDeclaration extends Declaration(mockPropertyLinkConnector,
    mockSessionRepo, mockBusinessRatesAttachmentService, withLinkingSession)

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))

    when(f.remove()(any())).thenReturn(Future.successful(()))
    f
  }

  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentService]

  lazy val mockPropertyLinkConnector = {
    val m = mock[PropertyLinkConnector]
    when(m.createPropertyLink()(any[LinkingSessionRequest[_]])).thenReturn(Future.successful(()))
    m
  }

  lazy val envelopeId: String = shortString

}
