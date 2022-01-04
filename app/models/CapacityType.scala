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

sealed trait CapacityType extends NamedEnum {
  val name: String
  val key = "capacity"
  override def toString = name
}

case object Occupier extends CapacityType {
  val name = "OCCUPIER"
}

case object Owner extends CapacityType {
  val name = "OWNER"
}

case object OwnerOccupier extends CapacityType {
  val name = "OWNER_OCCUPIER"
}

object CapacityType extends NamedEnumSupport[CapacityType] {
  implicit val format = EnumFormat(CapacityType)

  override def all: List[CapacityType] = List(Owner, Occupier, OwnerOccupier)
}
