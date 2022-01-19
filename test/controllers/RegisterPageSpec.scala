/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Mode}

class RegisterPageSpec @Inject()(configuration: Configuration) extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()
  implicit val mode = Mode.Test
  implicit val runConfig = configuration

  val applicationTestController = new Register(mockCustomErrorHandler, startView, startViewOld)

  "show" should "redirect to the organisation page" in {

    val result = applicationTestController.show()(FakeRequest())

    status(result) shouldBe SEE_OTHER

    redirectLocation(result) shouldBe Some(
      "http://localhost:8571/government-gateway-registration-frontend?accountType=organisation&continue=%2Fbusiness-rates-property-linking%2Fhome&origin=voa")

  }
  "choice" should "redirect to the government gateway registration page with the inputted form selection" in {

    val result = applicationTestController.choice()(
      FakeRequest().withFormUrlEncodedBody(
        "choice" -> "test123"
      ))

    status(result) shouldBe SEE_OTHER

    redirectLocation(result) shouldBe Some(
      "http://localhost:8571/government-gateway-registration-frontend?accountType=test123&continue=%2Fbusiness-rates-property-linking%2Fhome&origin=voa")

  }

  "continue" should "return the correct map including accountType" in {
    val testAccountType = "testAccountType"
    applicationTestController.continue(testAccountType) shouldBe Map(
      "accountType" -> Seq(testAccountType),
      "continue"    -> Seq("dashboard-url"), //fixme fix url if needed
      "origin"      -> Seq("voa"))
  }

  "choice" should "display a validation error if a choice is not selected" in {
    val result = applicationTestController.choice()(
      FakeRequest().withFormUrlEncodedBody(
        "choice" -> ""
      ))
    status(result) shouldBe BAD_REQUEST
  }

}
