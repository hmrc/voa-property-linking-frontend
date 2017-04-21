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
import models._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import utils.{HtmlPage, StubWithLinkingSession}
import play.api.test.Helpers._
import resources._
import _root_.session.LinkingSession

class ChooseEvidenceSpec extends ControllerSpec {

  private object TestChooseEvidence extends ChooseEvidence {
    val property = testProperty
    override val withLinkingSession = StubWithLinkingSession
  }

  lazy val testProperty: Property = arbitrary[Property]

  val request = FakeRequest().withSession(token)

  "The choose evidence page" must "ask the user whether they have a rates bill" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestChooseEvidence.show()(request)
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainRadioSelect("hasRatesBill", Seq("true", "false"))
  }

  it must "require the user to select whether they have a rates bill" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestChooseEvidence.submit()(request)
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainFieldErrors("hasRatesBill" -> "Please select an option")
  }

  it must "redirect to the rates bill upload page if the user has a rates bill" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadRatesBill.show().url)
  }

  it must "redirect to the other evidence page if the user does not have a rates bill" in {
    StubWithLinkingSession.stubSession(arbitrary[LinkingSession], arbitrary[DetailedIndividualAccount], arbitrary[GroupAccount])

    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadEvidence.show().url)
  }

}
