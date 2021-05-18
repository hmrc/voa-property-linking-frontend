/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationSpec extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()

  val applicationTestController = new Application(
    errorHandler = mockCustomErrorHandler,
    addUserToGGView = addUsertoGGView,
    invalidAccountTypeView = invalidAccountTypeView,
    startView = startView)

  "addUserToGG" should "display the add users to GG page" in {

    val result = applicationTestController.addUserToGG()(FakeRequest())

    status(result) mustBe OK

    val html = contentAsString(result)
    html must include("You should be aware that if you add an agent to your Government Gateway account")

  }

  "manageBusinessTaxAccount" should "redirect the user to the start page" in {

    val result = applicationTestController.manageBusinessTaxAccount()(FakeRequest())

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some("http://localhost:9020/business-account/manage-account")
  }

  "start" should "display the start page" in {

    val result = applicationTestController.start()(FakeRequest().withFormUrlEncodedBody("choice" -> "test"))

    status(result) mustBe OK

    val html = contentAsString(result)
    html must include("If you don’t have the details that you need to register")

  }

  "logout" should "redirect the user to the start page" in {

    val result = applicationTestController.logOut()(FakeRequest())

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some("/business-rates-property-linking")
  }

  "invalidAccountType" should "take the user to the invalid account page as they are unauthorized" in {

    val result = applicationTestController.invalidAccountType()(FakeRequest())

    status(result) mustBe UNAUTHORIZED

    val html = contentAsString(result)
    html must include("You’ve tried to register using an existing Agent Government Gateway account.")
  }

}
