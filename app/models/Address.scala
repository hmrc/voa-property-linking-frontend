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

package models

import play.api.libs.json.Json

case class Address(
      addressUnitId: Option[Long],
      line1: String,
      line2: String,
      line3: String,
      line4: String,
      postcode: String)

object Address {
  implicit val format = Json.format[Address]

  val empty = Address(addressUnitId = None, line1 = "", line2 = "", line3 = "", line4 = "", postcode = "")
}
