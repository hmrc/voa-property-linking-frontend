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

package models

sealed trait AgentPermission extends NamedEnum {
  override def key: String = "permission"
}

case object StartAndContinue extends AgentPermission {
  override def name: String = "START_AND_CONTINUE"
}

case object ContinueOnly extends AgentPermission {
  override def name: String = "CONTINUE_ONLY"
}

case object NotPermitted extends AgentPermission {
  override def name: String = "NOT_PERMITTED"
}

object AgentPermission extends NamedEnumSupport[AgentPermission] {
  implicit val format = EnumFormat(AgentPermission)
  override def all = Seq(StartAndContinue, ContinueOnly, NotPermitted)
  override def options = Seq(StartAndContinue, NotPermitted).map(_.name)
}
