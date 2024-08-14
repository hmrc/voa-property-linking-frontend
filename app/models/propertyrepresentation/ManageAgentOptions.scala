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
import play.api.libs.json.{Format, Json, OFormat}

sealed trait ManageAgentOptions extends NamedEnum {
  val name: String
  val key = "choice"
  override def toString = name
}

case object AssignToAllProperties extends ManageAgentOptions {
  val name = "assignToAllProperties"
}

case object AssignToSomeProperties extends ManageAgentOptions {
  val name = "assignToSomeProperties"
}

case object UnassignFromAllProperties extends ManageAgentOptions {
  val name = "unassignFromAllProperties"
}

case object UnassignFromSomeProperties extends ManageAgentOptions {
  val name = "unassignFromSomeProperties"
}

case object AssignToYourProperty extends ManageAgentOptions {
  val name = "assignToYourProperty"
}

case object UnassignFromYourProperty extends ManageAgentOptions {
  val name = "unassignFromYourProperty"
}

case object RemoveFromYourAccount extends ManageAgentOptions {
  val name = "removeFromYourAccount"
}

case object ChangeRatingList extends ManageAgentOptions {
  val name = "changeRatingList"
}

case object AssignToOneOrMoreProperties extends ManageAgentOptions {
  val name = "assignToOneOrMoreProperties"
}

case object UnassignFromOneOrMoreProperties extends ManageAgentOptions {
  val name = "unassignFromOneOrMoreProperties"
}

object ManageAgentOptions extends NamedEnumSupport[ManageAgentOptions] {

  implicit val format: Format[ManageAgentOptions] = EnumFormat(ManageAgentOptions)

  override def all: List[ManageAgentOptions] =
    List(
      AssignToAllProperties,
      AssignToSomeProperties,
      UnassignFromAllProperties,
      UnassignFromSomeProperties,
      AssignToYourProperty,
      UnassignFromYourProperty,
      RemoveFromYourAccount,
      AssignToOneOrMoreProperties,
      UnassignFromOneOrMoreProperties,
      ChangeRatingList
    )
}

case class ManageAgentOptionItem(value: ManageAgentOptions) {
  val id: String = value.name
  val titleMessageKeySuffix: String = value.name
}

object ManageAgentOptionItem {
  implicit val format: OFormat[ManageAgentOptionItem] = Json.format[ManageAgentOptionItem]
}
