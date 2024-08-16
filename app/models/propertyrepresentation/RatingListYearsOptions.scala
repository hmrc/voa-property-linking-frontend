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

package models.propertyrepresentation

import models.{EnumFormat, NamedEnum, NamedEnumSupport}
import play.api.libs.json.Format

sealed trait RatingListYearsOptions extends NamedEnum {
  val name: String
  val key = "choice"
  override def toString = name
}

case object ratingList2023 extends RatingListYearsOptions {
  val name = "2023"
}

case object ratingList2017 extends RatingListYearsOptions {
  val name = "2017"
}

object RatingListYearsOptions extends NamedEnumSupport[RatingListYearsOptions] {
  implicit val format: Format[RatingListYearsOptions] = EnumFormat(RatingListYearsOptions)

  override def all: List[RatingListYearsOptions] = List(ratingList2017, ratingList2023)
}
