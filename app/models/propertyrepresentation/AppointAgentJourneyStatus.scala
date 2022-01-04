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

sealed trait AppointAgentJourneyStatus extends NamedEnum {
  val name: String
  val key = "choice"
  override def toString = name
}

case object StartJourney extends AppointAgentJourneyStatus {
  val name = "StartJourney"
}

case object AgentSearched extends AppointAgentJourneyStatus {
  val name = "AgentSearched"
}

case object AgentSelected extends AppointAgentJourneyStatus {
  val name = "AgentSelected"
}

case object ManagingPropertySelected extends AppointAgentJourneyStatus {
  val name = "ManagingPropertySelected"
}

object AppointAgentJourneyStatus extends NamedEnumSupport[AppointAgentJourneyStatus] {
  implicit val format = EnumFormat(AppointAgentJourneyStatus)

  override def all: List[AppointAgentJourneyStatus] =
    List(StartJourney, AgentSearched, AgentSelected, ManagingPropertySelected)
}
