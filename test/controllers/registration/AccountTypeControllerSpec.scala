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

package controllers.registration

import controllers.VoaPropertyLinkingSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AccountTypeControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {
  implicit val request = FakeRequest()

  object testAccountTypeController extends AccountTypeController(mockCustomErrorHandler, accountTypeView)

  "The 'Account type' page" should "display page when url is hit" in {
    val res = testAccountTypeController.show()(request)

    status(res) shouldBe OK

    val html = contentAsString(res)
    html should include("What type of account would you like to create?")

  }

  "The 'Account type' page" should "require one of the radio buttons to be selected" in {
    val res = testAccountTypeController.submit()(request.withFormUrlEncodedBody("accountTypeIndividual" -> ""))
    status(res) shouldBe BAD_REQUEST

    val html = contentAsString(res)
    html should include("Select an option")

  }

  "The 'Account type' page" should "redirect the user to the individual registration page if 'individual' is selected" in {
    val res = testAccountTypeController.submit()(request.withFormUrlEncodedBody("accountTypeIndividual" -> "true"))
    status(res) shouldBe SEE_OTHER

    redirectLocation(res) shouldBe Some(
      "http://localhost:8571/government-gateway-registration-frontend?accountType=individual&continue=http%3A%2F%2Flocalhost%3A9542%2Fbusiness-rates-dashboard%2Fhome&origin=voa")

  }

  "The 'Account type' page" should "redirect the user to the organisation registration page if 'individual' is selected" in {
    val res = testAccountTypeController.submit()(request.withFormUrlEncodedBody("accountTypeIndividual" -> "false"))
    status(res) shouldBe SEE_OTHER

    redirectLocation(res) shouldBe Some(
      "http://localhost:8571/government-gateway-registration-frontend?accountType=organisation&continue=http%3A%2F%2Flocalhost%3A9542%2Fbusiness-rates-dashboard%2Fhome&origin=voa")
  }
}
