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

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._


class LoginPageSpec extends VoaPropertyLinkingSpec {
  implicit val request = FakeRequest()

  val applicationTestController = new Login(applicationConfig)

  "show" should "display the login page" in {

    val result = applicationTestController.show()(FakeRequest())

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some("http://localhost:9025/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9523%2Fbusiness-rates-property-linking%2Fhome&origin=voa")

  }

}