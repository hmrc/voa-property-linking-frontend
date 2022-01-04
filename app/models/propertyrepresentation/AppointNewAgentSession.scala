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

import play.api.libs.json._

sealed trait AppointNewAgentSession {
  val status: AppointAgentJourneyStatus
}

case class Start(status: AppointAgentJourneyStatus = StartJourney) extends AppointNewAgentSession

object Start {
  implicit val format: OFormat[Start] = Json.format[Start]
}

case class SearchedAgent(
      agentCode: Long,
      agentOrganisationName: String,
      agentAddress: String,
      status: AppointAgentJourneyStatus = AgentSearched)
    extends AppointNewAgentSession

object SearchedAgent {
  implicit val format: OFormat[SearchedAgent] = Json.format[SearchedAgent]
}

case class SelectedAgent(
      agentCode: Long,
      agentOrganisationName: String,
      agentAddress: String,
      isCorrectAgent: Boolean,
      status: AppointAgentJourneyStatus = AgentSelected)
    extends AppointNewAgentSession

object SelectedAgent {
  implicit val format: OFormat[SelectedAgent] = Json.format[SelectedAgent]

  def apply(searchedAgent: SearchedAgent, isTheCorrectAgent: Boolean): SelectedAgent =
    SelectedAgent(
      agentCode = searchedAgent.agentCode,
      agentOrganisationName = searchedAgent.agentOrganisationName,
      agentAddress = searchedAgent.agentAddress,
      isCorrectAgent = isTheCorrectAgent
    )
}

case class ManagingProperty(
      agentCode: Long,
      agentOrganisationName: String,
      agentAddress: String,
      isCorrectAgent: Boolean,
      managingPropertyChoice: String,
      status: AppointAgentJourneyStatus = ManagingPropertySelected)
    extends AppointNewAgentSession

object ManagingProperty {
  implicit val format: OFormat[ManagingProperty] = Json.format[ManagingProperty]

  def apply(selectedAgent: SelectedAgent, selection: String): ManagingProperty =
    ManagingProperty(
      agentCode = selectedAgent.agentCode,
      agentOrganisationName = selectedAgent.agentOrganisationName,
      agentAddress = selectedAgent.agentAddress,
      isCorrectAgent = selectedAgent.isCorrectAgent,
      managingPropertyChoice = selection
    )
}

object AppointNewAgentSession {

  val readers: Map[String, Reads[_ <: AppointNewAgentSession]] = Map(
    StartJourney.name             -> implicitly[Reads[Start]],
    AgentSearched.name            -> implicitly[Reads[SearchedAgent]],
    AgentSelected.name            -> implicitly[Reads[SelectedAgent]],
    ManagingPropertySelected.name -> implicitly[Reads[ManagingProperty]]
  )

  implicit val appointNewAgentSessionReads: Reads[AppointNewAgentSession] = new Reads[AppointNewAgentSession] {
    def reads(s: JsValue): JsResult[AppointNewAgentSession] = {
      val session = (s \ "status").as[String]
      val read = readers.get(session)
      read.map(_.reads(s)).getOrElse(JsError(s"Unsupported AppointNewAgentSession type: $session"))
    }
  }
}
