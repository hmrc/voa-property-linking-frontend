/*
 * Copyright 2020 HM Revenue & Customs
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

sealed trait ManageOnePropertyOptions extends NamedEnum {
  val name: String
  val key = "choice"
  override def toString = name
}

case object AsWellAsCurrent extends ManageOnePropertyOptions {
  val name = "AS_WELL_AS_CURRENT"
}

case object InsteadOfCurrent extends ManageOnePropertyOptions {
  val name = "INSTEAD_OF_CURRENT"
}

case object No extends ManageOnePropertyOptions {
  val name = "NO"
}

case object Yes extends ManageOnePropertyOptions {
  val name = "YES"
}

object ManageOnePropertyOptions extends NamedEnumSupport[ManageOnePropertyOptions] {
  implicit val format = EnumFormat(ManageOnePropertyOptions)

  override def all: List[ManageOnePropertyOptions] = List(AsWellAsCurrent, InsteadOfCurrent, No, Yes)
}
