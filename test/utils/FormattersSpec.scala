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

package utils

import controllers.VoaPropertyLinkingSpec

class FormattersSpec extends VoaPropertyLinkingSpec {

  "capitalised Address" must
    "format the address in camel case" in {
    val address = "THE OLD WAREHOUSE, CHALFONT STATION ROAD, LITTLE CHALFONT, AMERSHAM, BUCKS, HP7 9PS"
    Formatters.capitalisedAddress(address) mustBe "The Old Warehouse, Chalfont Station Road, Little Chalfont, Amersham, Bucks,  HP7 9PS"
  }
}