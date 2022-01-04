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

package models.searchApi

import models.{NamedEnum, NamedEnumSupport}

sealed trait AgentPropertiesFilter extends NamedEnum {
  override val key: String = "agentPropertyFilter"

  override def toString: String = name
}

object AgentPropertiesFilter extends NamedEnumSupport[AgentPropertiesFilter] {

  case object Both extends AgentPropertiesFilter {
    override val name: String = "BOTH"
  }

  case object No extends AgentPropertiesFilter {
    override val name: String = "NO"
  }

  case object Yes extends AgentPropertiesFilter {
    override val name: String = "YES"
  }

  override def all: Seq[AgentPropertiesFilter] = Seq(Both, Yes, No)
}
