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

package controllers.propertyLinking

import connectors.fileUpload.FileUploadConnector
import controllers.ControllerSpec
import models.{DetailedIndividualAccount, GroupAccount, LinkBasis}
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{HtmlPage, StubLinkingSessionRepository, StubPropertyLinkConnector, StubWithLinkingSession}
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import _root_.session.{LinkingSession, LinkingSessionRequest}
import connectors.EnvelopeConnector
import connectors.propertyLinking.PropertyLinkConnector
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.Future

class DeclarationSpec extends ControllerSpec with MockitoSugar {

  private object TestDeclaration extends Declaration(mockFileUploadConnector) {
    override val linkingSessionRepo = new StubLinkingSessionRepository
    override val withLinkingSession = StubWithLinkingSession
    override val propertyLinks = mockPropertyLinkConnector
  }

  lazy val mockFileUploadConnector = {
    val m = mock[EnvelopeConnector]
    when(m.closeEnvelope(matching(envelopeId))(any[HeaderCarrier])).thenReturn(Future.successful(""))
    m
  }

  lazy val mockPropertyLinkConnector = {
    val m = mock[PropertyLinkConnector]
    when(m.linkToProperty(any[LinkBasis])(any[LinkingSessionRequest[_]])).thenReturn(Future.successful(()))
    m
  }

  lazy val envelopeId: String = shortString

  "The declaration page" should "include a checkbox to allow the user to accept the declaration" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.show()(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.select("input[type=checkbox]").asScala.map(_.id) must contain ("declaration")
  }

  it should "require the user to accept the declaration to continue" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest())
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainSummaryErrors(("declaration", "Declaration", "You must agree to the declaration to continue"))
  }

  it should "submit the property link if the user accepts the declaration" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession].retryUntil(_.linkBasis.isDefined).copy(envelopeId = envelopeId)

    StubWithLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) mustBe OK

    verify(mockPropertyLinkConnector, times(1)).linkToProperty(matching(linkingSession.linkBasis.get))(any[LinkingSessionRequest[_]])
  }

  "The confirmation page" should "display the submission ID" in {
    val linkingSession: LinkingSession = arbitrary[LinkingSession].copy(envelopeId = envelopeId)
    StubWithLinkingSession.stubSession(linkingSession, arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) mustBe OK

    contentAsString(res) must include (linkingSession.submissionId)
  }
}
