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

import play.api.libs.json.Json

case class AppointAgent(
      agentCode: Option[Long],
      agentCodeRadio: String
) {
  def getAgentCode(): Long = agentCode match {
    case Some(code) => code
    case None       => agentCodeRadio.toLong
  }
}

object AppointAgent {
  implicit val format = Json.format[AppointAgent]
}

case class AgentAppointBulkAction(
      agentCode: Long,
      propertyLinkIds: List[String],
      backLinkUrl: String
)

object AgentAppointBulkAction {

  def unpack(arg: AgentAppointBulkAction): Option[(Long, List[String], String)] =
    Some((arg.agentCode, arg.propertyLinkIds, arg.backLinkUrl))

  implicit val format = Json.format[AgentAppointBulkAction]
}

case class AgentRevokeBulkAction(
      agentCode: Long,
      propertyLinkIds: List[String],
      backLinkUrl: String
)

object AgentRevokeBulkAction {

  def unpack(arg: AgentRevokeBulkAction): Option[(Long, List[String], String)] =
    Some((arg.agentCode, arg.propertyLinkIds, arg.backLinkUrl))

  implicit val format = Json.format[AgentRevokeBulkAction]
}
