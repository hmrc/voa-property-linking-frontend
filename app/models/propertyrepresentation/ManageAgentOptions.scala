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
import play.api.libs.json.Json

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

case object RemoveFromYourAccount extends ManageAgentOptions {
  val name = "removeFromYourAccount"
}

object ManageAgentOptions extends NamedEnumSupport[ManageAgentOptions] {
  implicit val format = EnumFormat(ManageAgentOptions)

  override def all: List[ManageAgentOptions] =
    List(
      AssignToAllProperties,
      AssignToSomeProperties,
      UnassignFromAllProperties,
      UnassignFromSomeProperties,
      AssignToYourProperty,
      RemoveFromYourAccount)

  val onePropertyLinkNoAssignedAgentsOptions = List(
    ManageAgentOptionItem(s"${AssignToYourProperty.name}"),
    ManageAgentOptionItem(s"${RemoveFromYourAccount.name}")
  )

  val multiplePropertyLinksNoAssignedAgentsOptions = List(
    ManageAgentOptionItem(s"${AssignToAllProperties.name}"),
    ManageAgentOptionItem(s"${AssignToSomeProperties.name}"),
    ManageAgentOptionItem(s"${RemoveFromYourAccount.name}")
  )

  val multiplePropertyLinksAgentAssignedToSomeOptions = List(
    ManageAgentOptionItem(s"${AssignToAllProperties.name}"),
    ManageAgentOptionItem(s"${AssignToSomeProperties.name}"),
    ManageAgentOptionItem(s"${UnassignFromAllProperties.name}"),
    ManageAgentOptionItem(s"${UnassignFromSomeProperties.name}")
  )

  val multiplePropertyLinksAgentAssignedToAllOptions = List(
    ManageAgentOptionItem(s"${UnassignFromAllProperties.name}"),
    ManageAgentOptionItem(s"${UnassignFromSomeProperties.name}")
  )
}

case class ManageAgentOptionItem(value: String) {
  val id: String = value
  val titleMessageKeySuffix: String = value
}

object ManageAgentOptionItem {
  implicit val format = Json.format[ManageAgentOptionItem]
}
