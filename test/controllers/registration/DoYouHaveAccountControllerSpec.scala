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

class DoYouHaveAccountControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {
  implicit val request = FakeRequest()

  private lazy val loginPage = controllers.routes.Login.show.url
  private lazy val accountTypePage = controllers.registration.routes.AccountTypeController.show().url

  object testDoYouHaveAccountController extends DoYouHaveAccountController(mockCustomErrorHandler, doYouHaveAccountView)

  "The 'Do you have an account' page" should "display page when url is hit" in {
    val res = testDoYouHaveAccountController.show()(request)

    status(res) shouldBe OK

    val html = contentAsString(res)
    html should include("Do you already have a Government Gateway account?")

  }

  "The 'Do you have an account' page" should "require one of the radio buttons to be selected" in {
    val res = testDoYouHaveAccountController.submit()(request.withFormUrlEncodedBody("hasAccount" -> ""))
    status(res) shouldBe BAD_REQUEST

    val html = contentAsString(res)
    html should include("Select an option")

  }

  "The 'Do you have account' page" should "redirect the user to the BAS sign in page if 'yes' is selected" in {
    val res = testDoYouHaveAccountController.submit()(request.withFormUrlEncodedBody("hasAccount" -> "true"))
    status(res) shouldBe SEE_OTHER

    redirectLocation(res) shouldBe Some(loginPage)

  }

  "The 'Do you have account' page" should "redirect the user to the 'account type' question page if 'yes' is selected" in {
    val res = testDoYouHaveAccountController.submit()(request.withFormUrlEncodedBody("hasAccount" -> "false"))
    status(res) shouldBe SEE_OTHER

    redirectLocation(res) shouldBe Some(accountTypePage)

  }

}
