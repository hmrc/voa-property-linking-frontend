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

import controllers.ControllerSpec
import models._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import utils.{HtmlPage, StubWithLinkingSession}

import scala.concurrent.Future

class ChooseEvidenceSpec extends ControllerSpec with MockitoSugar{

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)
  private class TestChooseEvidence (withLinkingSession: StubWithLinkingSession) extends ChooseEvidence(withLinkingSession) {
    val property = testProperty
  }
  private val testChooseEvidence = new TestChooseEvidence(withLinkingSession)

  lazy val testProperty: Property = arbitrary[Property]

  val request = FakeRequest().withSession(token)

  "The choose evidence page" must "ask the user whether they have a rates bill" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = testChooseEvidence.show()(request)
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainRadioSelect("hasRatesBill", Seq("true", "false"))
  }

  it must "require the user to select whether they have a rates bill" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = testChooseEvidence.submit()(request)
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainFieldErrors("hasRatesBill" -> "Please select an option")
  }

  it must "redirect to the rates bill upload page if the user has a rates bill" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = testChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadRatesBill.show().url)
  }

  it must "redirect to the other evidence page if the user does not have a rates bill" in {
    withLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = testChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadEvidence.show().url)
  }

}
