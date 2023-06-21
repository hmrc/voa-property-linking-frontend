/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}

import scala.util.Try

case class AgentAppointmentChangeRequest(
      agentRepresentativeCode: Long,
      action: String,
      scope: String,
      propertyLinkIds: Option[List[String]],
      listYears: Option[List[String]] = None)

object AgentAppointmentChangeRequest {

  implicit val format: OFormat[AgentAppointmentChangeRequest] = Json.format

  val submitAgentAppointmentRequest: Form[AgentAppointmentChangeRequest] =
    Form(
      mapping(
        "agentCode"       -> longNumber,
        "action"          -> text.verifying(s => Try(AppointmentAction.withName(s)).toOption.isDefined),
        "scope"           -> text.verifying(s => Try(AppointmentScope.withName(s)).toOption.isDefined),
        "propertyLinkIds" -> optional(list(text)),
        "listYears"       -> optional(list(text))
      )(AgentAppointmentChangeRequest.apply)(AgentAppointmentChangeRequest.unapply)
    )
}
