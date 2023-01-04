/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class ApplicationSpec extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()

  val applicationTestController = new Application(
    errorHandler = mockCustomErrorHandler,
    addUserToGGView = addUsertoGGView,
    invalidAccountTypeView = invalidAccountTypeView,
    startView = startView,
    startViewOldJourney = startViewOld
  )

  "addUserToGG" should "display the add users to GG page" in {

    val result = applicationTestController.addUserToGG()(FakeRequest())

    status(result) shouldBe OK

    val html = contentAsString(result)
    html should include("You should be aware that if you add an agent to your Government Gateway account")

  }

  "manageBusinessTaxAccount" should "redirect the user to the start page" in {

    val result = applicationTestController.manageBusinessTaxAccount()(FakeRequest())

    status(result) shouldBe SEE_OTHER

    redirectLocation(result) shouldBe Some("http://localhost:9020/business-account/manage-account")
  }

  "start" should "display the start page" in {

    val result = applicationTestController.start()(FakeRequest().withFormUrlEncodedBody("choice" -> "test"))

    status(result) shouldBe OK

    val html = contentAsString(result)
    html should include("Before you start")

  }

  "logout" should "redirect the user to the start page" in {

    val result = applicationTestController.logOut()(FakeRequest())

    status(result) shouldBe SEE_OTHER

    redirectLocation(result) shouldBe Some("/business-rates-property-linking")
  }

  "invalidAccountType" should "take the user to the invalid account page as they are unauthorized" in {

    val result = applicationTestController.invalidAccountType()(FakeRequest())

    status(result) shouldBe UNAUTHORIZED

    val html = contentAsString(result)
    html should include("You’ve tried to register using an existing Agent Government Gateway account.")
  }

  "setDefaultLanguage" should "redirect the user to REFERER" in {
    val refererUrl = "/test-url/"
    val result = applicationTestController.setDefaultLanguage(FakeRequest().withHeaders("REFERER" -> refererUrl))
    verifyLanguageSwitch(result, refererUrl)
  }

  "setDefaultLanguage" should "redirect the user HOME when REFERER not available" in {
    val result: Future[Result] = applicationTestController.setDefaultLanguage(FakeRequest())
    verifyLanguageSwitch(result)
  }
  "displayWelsh" should "redirect the user to REFERER" in {
    val refererUrl = "/test-url/"
    val result: Future[Result] =
      applicationTestController.displayWelsh(FakeRequest().withHeaders("REFERER" -> refererUrl))
    verifyLanguageSwitch(result, refererUrl, lang = "cy")
  }
  "displayWelsh" should "redirect the user HOME when REFERER not available" in {
    val refererUrl = "/test-url/"
    val result: Future[Result] = applicationTestController.displayWelsh(FakeRequest())
    verifyLanguageSwitch(result, lang = "cy")
  }
  private def verifyLanguageSwitch(
        result: Future[Result],
        refererUrl: String = applicationConfig.dashboardUrl("home"),
        lang: String = ""
  ) = {
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(refererUrl)
    cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some(lang)
  }

}
