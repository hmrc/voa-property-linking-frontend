/*
 * Copyright 2016 HM Revenue & Customs
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

case class PropertyAddress(line1: String, line2: String, line3: String, postcode: String) {
  override def toString = Seq(line1, line2, line3, postcode).filter(_.nonEmpty).mkString(", ")
}

object PropertyAddress {
  implicit val addressFormat = Json.format[PropertyAddress]
}

case class Property(uarn: Long, billingAuthorityReference: String, address: PropertyAddress, isSelfCertifiable: Boolean,
                    specialCategoryCode: String, description: String, bulkClassIndicator: String)