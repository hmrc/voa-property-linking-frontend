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

sealed trait ManageMultiplePropertiesOptions extends NamedEnum {
  val name: String
  val key = "choice"
  override def toString = name
}

case object All extends ManageMultiplePropertiesOptions {
  val name = "ALL"
}

case object None extends ManageMultiplePropertiesOptions {
  val name = "NONE"
}

case object ChooseFromList extends ManageMultiplePropertiesOptions {
  val name = "CHOOSE_FROM_LIST"
}

object ManageMultiplePropertiesOptions extends NamedEnumSupport[ManageMultiplePropertiesOptions] {
  implicit val format = EnumFormat(ManageMultiplePropertiesOptions)

  override def all: List[ManageMultiplePropertiesOptions] = List(All, None, ChooseFromList)
}
