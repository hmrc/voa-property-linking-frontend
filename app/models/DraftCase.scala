/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DraftCase(id: String,
                     url: String,
                     address: PropertyAddress,
                     effectiveDate: LocalDate,
                     checkType: String,
                     expirationDate: LocalDate,
                     propertyLinkId: Long,
                     assessmentRef: Long,
                     baRef: String)

object DraftCase {
  implicit val rds: Reads[DraftCase] = Json.reads[DraftCase]

  private implicit lazy val checkAddressReads: Reads[PropertyAddress] = (
    (__ \ "name").readNullable[String] and
      (__ \ "firm").readNullable[String] and
      (__ \ "number").readNullable[String] and
      (__ \ "street").readNullable[String] and
      (__ \ "subStreet1").readNullable[String] and
      (__ \ "subStreet2").readNullable[String] and
      (__ \ "subStreet3").readNullable[String] and
      (__ \ "postalDistrict").readNullable[String] and
      (__ \ "town").readNullable[String] and
      (__ \ "county").readNullable[String] and
      (__ \ "postcode").readNullable[String]
    ) (mkAddress _)

  // scalastyle:off
  private def mkAddress(l1: Option[String],
                        l2: Option[String],
                        l3: Option[String],
                        l4: Option[String],
                        l5: Option[String],
                        l6: Option[String],
                        l7: Option[String],
                        l8: Option[String],
                        l9: Option[String],
                        l10: Option[String],
                        postcode: Option[String]): PropertyAddress = {
    val lines = Seq(l1, l2, l3, l4, l5, l6, l7, l8, l9, l10) collect { case Some(l) => l }
    PropertyAddress(lines, postcode.getOrElse(""))
  }
}
