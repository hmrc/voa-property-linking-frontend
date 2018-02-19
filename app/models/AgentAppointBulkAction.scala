/*
 * Copyright 2018 HM Revenue & Customs
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

case class AgentAppointBulkAction(
                                   checkPermission: AgentPermission,
                                   challengePermission: AgentPermission,
                                   propertyLinkIds: List[String]
                                   )

object AgentAppointBulkAction {
  def apply(
             checkPermission: String,
             challengePermission: String,
             propertyLinkIds: List[String]
           ): AgentAppointBulkAction = new AgentAppointBulkAction(
                                              AgentPermission.fromName(checkPermission).getOrElse(StartAndContinue),
                                              AgentPermission.fromName(challengePermission).getOrElse(StartAndContinue),
                                              propertyLinkIds)

  def unpack(arg: AgentAppointBulkAction): Option[(String, String, List[String])] =
    Some((arg.checkPermission.name, arg.challengePermission.name, arg.propertyLinkIds))

  implicit val format = Json.format[AgentAppointBulkAction]
}

