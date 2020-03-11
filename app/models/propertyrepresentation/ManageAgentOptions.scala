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
import play.api.libs.json.{Json, OFormat}

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
    List(AssignToAllProperties, AssignToSomeProperties, UnassignFromAllProperties,
      UnassignFromSomeProperties, AssignToYourProperty, RemoveFromYourAccount)

  val onePropertyLinkNoAssignedAgentsOptions = List(
    ManageAgentOptionItem(id = "manageAgent-assignToYourProperty", value = "assignToYourProperty", titleMessageKeySuffix = "assignToYourProperty"),
    ManageAgentOptionItem(id = "manageAgent-removeFromYourAccount", value = "removeFromYourAccount", titleMessageKeySuffix = "removeFromYourAccount")
  )

  val multiplePropertyLinksNoAssignedAgentsOptions = List(
    ManageAgentOptionItem(id = "manageAgent-assignToAllProperties", value = "assignToAllProperties", titleMessageKeySuffix = "assignToAllProperties"),
    ManageAgentOptionItem(id = "manageAgent-assignToSomeProperties", value = "assignToSomeProperties", titleMessageKeySuffix = "assignToSomeProperties"),
    ManageAgentOptionItem(id = "manageAgent-removeFromYourAccount", value = "removeFromYourAccount", titleMessageKeySuffix = "removeFromYourAccount")
  )

  val multiplePropertyLinksAgentAssignedToSomeOptions = List(
    ManageAgentOptionItem(id = "manageAgent-assignToAllProperties", value = "assignToAllProperties", titleMessageKeySuffix = "assignToAllProperties"),
    ManageAgentOptionItem(id = "manageAgent-assignToSomeProperties", value = "assignToSomeProperties", titleMessageKeySuffix = "assignToSomeProperties"),
    ManageAgentOptionItem(id = "manageAgent-unassignFromAllProperties", value = "unassignFromAllProperties", titleMessageKeySuffix = "unassignFromAllProperties"),
    ManageAgentOptionItem(id = "manageAgent-unassignFromSomeProperties", value = "unassignFromSomeProperties", titleMessageKeySuffix = "unassignFromSomeProperties")
  )

  val multiplePropertyLinksAgentAssignedToAllOptions = List(
    ManageAgentOptionItem(id = "manageAgent-unassignFromAllProperties", value = "unassignFromAllProperties", titleMessageKeySuffix = "unassignFromAllProperties"),
    ManageAgentOptionItem(id = "manageAgent-unassignFromSomeProperties", value = "unassignFromSomeProperties", titleMessageKeySuffix = "unassignFromSomeProperties")
  )
}

case class ManageAgentOptionItem(id: String, value: String, titleMessageKeySuffix: String)

object ManageAgentOptionItem {
  implicit val format = Json.format[ManageAgentOptionItem]
}
