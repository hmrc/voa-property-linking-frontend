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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.downtimePage

class DowntimePageSpec extends VoaPropertyLinkingSpec {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val applicationTestController =
    new DowntimePage(
      mockCustomErrorHandler,
      preAuthenticatedStaticPage(),
      stubMessagesControllerComponents(),
      new downtimePage(mainLayout))

  "plannedImprovements" should "display the downtime page when user is logged in" in {

    val result = applicationTestController.plannedImprovements()(FakeRequest())

    status(result) shouldBe OK
    verifyLoggedIn(Jsoup.parse(contentAsString(result)), "Service unavailable - Valuation Office Agency - GOV.UK")

  }

  "plannedImprovements" should "display the downtime page when user is NOT logged in" in {

    val result = new DowntimePage(
      mockCustomErrorHandler,
      preAuthenticatedStaticPage(accounts = None),
      stubMessagesControllerComponents(),
      new downtimePage(mainLayout)).plannedImprovements()(FakeRequest())

    status(result) shouldBe OK
    verifyNotLoggedIn(Jsoup.parse(contentAsString(result)), "Service unavailable - Valuation Office Agency - GOV.UK")

  }

}
