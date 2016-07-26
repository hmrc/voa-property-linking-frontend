/*
 * Copyright 2016 HM Revenue & Customs
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
  val key = "capacityType"
  override def toString = name
}

case object Occupier extends CapacityType {
  val name = "occupier"
}

case object OwnerLandlord extends CapacityType {
  val name = "ownerlandlord"
}

case object PreviousOccupier extends CapacityType {
  val name = "previousOccupier"
}

object CapacityType extends NamedEnumSupport[CapacityType] {
  def unapply(s: String) = s match {
    case Occupier.name => Some(Occupier)
    case OwnerLandlord.name => Some(OwnerLandlord)
    case PreviousOccupier.name => Some(PreviousOccupier)
    case _ => None
  }

  def options = Seq(Occupier.name, OwnerLandlord.name, PreviousOccupier.name)

  override def all: List[CapacityType] = List(Occupier, OwnerLandlord, PreviousOccupier)
}
