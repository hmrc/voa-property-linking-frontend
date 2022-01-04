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

package models.propertyrepresentation

import models.{EnumFormat, NamedEnum, NamedEnumSupport}

sealed trait AddAgentOptions extends NamedEnum {
  val name: String
  val key = "choice"
  override def toString = name
}

case object All extends AddAgentOptions {
  val name = "all"
}

case object NoProperties extends AddAgentOptions {
  val name = "none"
}

case object ChooseFromList extends AddAgentOptions {
  val name = "choose_from_list"
}

case object No extends AddAgentOptions {
  val name = "no"
}

case object Yes extends AddAgentOptions {
  val name = "all"
}

object AddAgentOptions extends NamedEnumSupport[AddAgentOptions] {
  implicit val format = EnumFormat(AddAgentOptions)

  override def all: List[AddAgentOptions] = List(All, NoProperties, ChooseFromList, Yes, No)
}
