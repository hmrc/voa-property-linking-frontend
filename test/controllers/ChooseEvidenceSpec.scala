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
import connectors.CapacityDeclaration
import models._
import play.api.test.FakeRequest
import utils.{HtmlPage, StubWithLinkingSession}
import play.api.test.Helpers._

class ChooseEvidenceSpec extends ControllerSpec {
  import TestData._

  private object TestChooseEvidence extends ChooseEvidence {
    override val withLinkingSession = new StubWithLinkingSession(property, declaration, individual)
  }

  val request = FakeRequest().withSession(token)

  "The choose evidence page" must "ask the user whether they have a rates bill" in {
    val res = TestChooseEvidence.show()(request)
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainRadioSelect("hasRatesBill", Seq("true", "false"))
  }

  it must "require the user to select whether they have a rates bill" in {
    val res = TestChooseEvidence.submit()(request)
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainFieldErrors("hasRatesBill" -> "This field is required")
  }

  it must "redirect to the rates bill upload page if the user has a rates bill" in {
    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "true"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadRatesBill.show().url)
  }

  it must "redirect to the other evidence page if the user does not have a rates bill" in {
    val res = TestChooseEvidence.submit()(request.withFormUrlEncodedBody("hasRatesBill" -> "false"))
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.UploadEvidence.show().url)
  }

  private object TestData {
    val property = Property(1234567L, "8901234", PropertyAddress(Seq("1", "2", "3"), "AB1 2CD"), false, "123", "a thing", "S")
    val declaration = CapacityDeclaration(Owner, true, None, true, None)
    val individual = DetailedIndividualAccount("externalId", "trustId", 111, 111,
      IndividualDetails("fistName", "lastName", "email", "phone1", None, SimpleAddress(None, "line1", "line2", "line3", "line4", "postcode"))
    )
  }
}
