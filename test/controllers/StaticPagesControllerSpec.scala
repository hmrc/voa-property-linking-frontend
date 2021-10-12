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

import models.DetailedIndividualAccount
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{StubIndividualAccountConnector, _}
import views.html.createAccount.termsAndConditions

class StaticPagesControllerSpec extends VoaPropertyLinkingSpec {

  private object TestStaticPagesController
      extends StaticPagesController(stubMessagesControllerComponents(), termsAndConditionsView)

  "Static page controller" should
    "return terms and conditions page and returns 200" in {
    val res = TestStaticPagesController.termsAndConditions()(FakeRequest())
    status(res) mustBe OK
  }

}