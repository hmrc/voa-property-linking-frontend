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

import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.AllMocks
import utils.StubAddresses

class AddressLookupSpec extends VoaPropertyLinkingSpec with AllMocks {
  implicit val request = FakeRequest()

  object TestAddressLookupController
      extends AddressLookup(mockCustomErrorHandler, StubAddresses, stubMessagesControllerComponents())

  behavior of "Address lookup"

  it should "eventually return Ok for correctly formatted postcode" in {
    val res = TestAddressLookupController.findByPostcode("AB1 1BA")(request)
    status(res) shouldBe OK
  }

  it should "return OK even if postcode has no space" in {
    val res = TestAddressLookupController.findByPostcode("AB11BA")(request)
    status(res) shouldBe OK
  }

  it should "return 404 for postcode with no result" in {
    val res = TestAddressLookupController.findByPostcode(StubAddresses.noResultPostcode)(request)
    status(res) shouldBe NOT_FOUND
  }

}
