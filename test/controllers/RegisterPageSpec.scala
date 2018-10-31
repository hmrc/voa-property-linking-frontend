/*
 * Copyright 2018 HM Revenue & Customs
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

import auth.VoaAction
import play.api.test.FakeRequest
import play.api.test.Helpers._


class RegisterPageSpec extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()

  val mockVoaAction = mock[VoaAction]

  val applicationTestController = new Register(mockVoaAction)

  "show" should "redirect to the organisation page" in {

    val result = applicationTestController.show()(FakeRequest())

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some("http://localhost:8571/government-gateway-registration-frontend?accountType=organisation&continue=%2Fbusiness-rates-property-linking%2Fhome&origin=voa")

  }
  "choice" should "redirect to the government gateway registration page with the inputted form selection" in {

    val result = applicationTestController.choice()(FakeRequest().withFormUrlEncodedBody(
      "choice" -> "test123"
    ))

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some("http://localhost:8571/government-gateway-registration-frontend?accountType=test123&continue=%2Fbusiness-rates-property-linking%2Fhome&origin=voa")

  }

  "continue" should "return the correct map including accountType" in {
    val testAccountType = "testAccountType"
    applicationTestController.continue(testAccountType) mustBe  Map("accountType" -> Seq(testAccountType), "continue" -> Seq(routes.Dashboard.home().url), "origin" -> Seq("voa"))
  }

  "display a validation error if a choice is not selected"

  val result = applicationTestController.choice()(FakeRequest().withFormUrlEncodedBody(
    "choice" -> ""
  ))

  status(result) mustBe BAD_REQUEST


}