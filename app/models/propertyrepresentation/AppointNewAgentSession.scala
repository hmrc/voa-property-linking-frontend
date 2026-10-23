/*
 * Copyright 2024 HM Revenue & Customs
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

import models.propertyrepresentation.AppointmentScope.AppointmentScope
import play.api.libs.json._

sealed trait AppointNewAgentSession {
  val status: AppointAgentJourneyStatus
  val backLink: Option[String]
}

case class Start(status: AppointAgentJourneyStatus = StartJourney, backLink: Option[String])
    extends AppointNewAgentSession

object Start {
  implicit val format: OFormat[Start] = Json.format[Start]
}

case class AppointNewAgentDetails(
      agentCode: Long,
      agentOrganisationName: String,
      agentAddress: String,
      status: AppointAgentJourneyStatus,
      backLink: Option[String] = None,
      isCorrectAgent: Option[Boolean] = None,
      ratingLists: Seq[String] = Seq.empty,
      managingPropertyChoice: Option[AddAgentOptions] = None,
      singleProperty: Boolean = false,
      totalPropertySelectionSize: Int = 0,
      propertySelectedSize: Int = 0,
      appointmentScope: Option[AppointmentScope] = None
) extends AppointNewAgentSession

object AppointNewAgentDetails {
  implicit val format: OFormat[AppointNewAgentDetails] =
    Json.using[Json.WithDefaultValues].format[AppointNewAgentDetails]
}

object AppointNewAgentSession {

  val readers: Map[String, Reads[_ <: AppointNewAgentSession]] = Map(
    StartJourney.name             -> implicitly[Reads[Start]],
    AgentSearched.name            -> implicitly[Reads[AppointNewAgentDetails]],
    AgentSelected.name            -> implicitly[Reads[AppointNewAgentDetails]],
    ManagingPropertySelected.name -> implicitly[Reads[AppointNewAgentDetails]]
  )

  implicit val appointNewAgentSessionReads: Reads[AppointNewAgentSession] = new Reads[AppointNewAgentSession] {
    def reads(s: JsValue): JsResult[AppointNewAgentSession] = {
      val session = (s \ "status").as[String]
      val read = readers.get(session)
      read.map(_.reads(s)).getOrElse(JsError(s"Unsupported AppointNewAgentSession type: $session"))
    }
  }
}
