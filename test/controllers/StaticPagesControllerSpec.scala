/*
 * Copyright 2024 HM Revenue & Customs
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

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._

class StaticPagesControllerSpec extends VoaPropertyLinkingSpec {

  private object TestStaticPagesController extends StaticPagesController(
        mockCustomErrorHandler,
        stubMessagesControllerComponents(),
        preAuthenticatedStaticPage(),
        termsAndConditionsView
      )

  "Static page controller" should
    "return terms and conditions page and returns 200 when user is logged in" in {
      val res = TestStaticPagesController.termsAndConditions()(FakeRequest())
      status(res) shouldBe OK
      verifyLoggedIn(Jsoup.parse(contentAsString(res)), "Terms and conditions - Valuation Office Agency - GOV.UK")
    }

  "Static page controller" should
    "return terms and conditions page and returns 200 when user is NOT logged in" in {
      val res = new StaticPagesController(
        mockCustomErrorHandler,
        stubMessagesControllerComponents(),
        preAuthenticatedStaticPage(accounts = None),
        termsAndConditionsView
      ).termsAndConditions()(FakeRequest())
      status(res) shouldBe OK
      verifyNotLoggedIn(Jsoup.parse(contentAsString(res)), "Terms and conditions - Valuation Office Agency - GOV.UK")
    }

}
