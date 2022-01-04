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

import models.DetailedIndividualAccount
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._
import utils.StubIndividualAccountConnector

class KeepAliveControllerSpec extends VoaPropertyLinkingSpec {

  private object TestRegistrationController$
      extends KeepAliveController(preAuthenticatedActionBuilders(), stubMessagesControllerComponents())

  "Keep Alive User Session" should
    "return keep alive returns 200" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubIndividualAccountConnector.stubAccount(
      arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = externalId))
    val res = TestRegistrationController$.keepAlive()(FakeRequest())
    status(res) shouldBe OK
  }

}
