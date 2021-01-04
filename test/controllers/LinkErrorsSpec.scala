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

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.AllMocks

class LinkErrorsSpec extends VoaPropertyLinkingSpec with AllMocks {
  implicit val request = FakeRequest()

  val applicationTestController = new LinkErrors(mockCustomErrorHandler, stubMessagesControllerComponents())

  "manualVerificationRequired" should "display the manual verification required page" in {

    val result = applicationTestController.manualVerificationRequired()(FakeRequest())

    status(result) mustBe OK

    val html = contentAsString(result)
    html must include("We were unable to link you to the requested property. We need to manually verify your request.")
  }

  "conflict" should "display the conflict page" in {

    val result = applicationTestController.conflict()(FakeRequest())

    status(result) mustBe OK

    val html = Jsoup.parse(contentAsString(result))
    html.title mustBe "Property already linked to another customer record"
  }

}
