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

package models

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DraftCase(
      id: String,
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
  )(mkAddress _)

  private def mkAddress(
        name: Option[String],
        firm: Option[String],
        number: Option[String],
        street: Option[String],
        subStreet1: Option[String],
        subStreet2: Option[String],
        subStreet3: Option[String],
        postalDistrict: Option[String],
        town: Option[String],
        county: Option[String],
        postcode: Option[String]): PropertyAddress = {

    val firmNameNumber: Option[String] = Seq(firm, number, name).flatten.mkString(" ") match {
      case ""  => None
      case fnn => Some(fnn)
    }

    val lines = Seq(firmNameNumber, subStreet3, subStreet2, subStreet1, street, postalDistrict, town, county) collect {
      case Some(l) => l
    }

    PropertyAddress(lines, postcode.getOrElse(""))
  }
}
