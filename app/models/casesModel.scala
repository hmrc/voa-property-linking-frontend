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

import java.time.{LocalDate, LocalDateTime}
import play.api.libs.json.Json
import play.api.libs.json.Json._

sealed case class CheckStatus(value: String)

sealed trait CheckCasesResponse{
  def filterTotal:Int
}

object CheckStatus {
  object Open extends CheckStatus("OPEN")
  object Pending extends CheckStatus("PENDING")
  object Closed extends CheckStatus("CLOSED")
  object ActionRequired extends CheckStatus("ACTIONREQUIRED")
  object Paused extends CheckStatus("PAUSED")
  object Cancelled extends CheckStatus("CANCELLED")

  val values = Seq(Open,Pending,Closed,ActionRequired,Paused,Cancelled)
}


case class OwnerCheckCasesResponse(start: Int,
                                   size: Int,
                                   filterTotal: Int,
                                   total: Int,
                                   checkCases: List[OwnerCheckCase]) extends CheckCasesResponse{

  require(start >= 1, "start cannot be less than 1")
  require(size >= 0, "size cannot be a negative number")
  require(size <= 1000, "size cannot be more than 1000")
  require(total >= 0, "total cannot be a negative number")
}

object OwnerCheckCasesResponse {
  implicit val formatCheckStatus = Json.format[CheckStatus]
  implicit val formatAgent = Json.format[Agent]
  implicit val formatOwnerCheckCase = Json.format[OwnerCheckCase]
  implicit val formatCheckCasesResponse = Json.format[OwnerCheckCasesResponse]
}

case class AgentCheckCasesResponse(start: Int,
                                   size: Int,
                                   filterTotal: Int,
                                   total: Int,
                                   checkCases: List[AgentCheckCase]) extends CheckCasesResponse{

  require(start >= 1, "start cannot be less than 1")
  require(size >= 0, "size cannot be a negative number")
  require(size <= 1000, "size cannot be more than 1000")
  require(total >= 0, "total cannot be a negative number")
}

object AgentCheckCasesResponse {
  implicit val formatCheckStatus = Json.format[CheckStatus]
  implicit val formatClient = Json.format[Client]
  implicit val formatAgentCheckCase = Json.format[AgentCheckCase]
  implicit val formatCheckCasesResponse = Json.format[AgentCheckCasesResponse]

}

case class Agent(organisationId: Long, code: Long, organisationName: String)

object Agent {
  implicit val formatAgent = Json.format[Agent]
}

case class Client(organisationId: Long, organisationName: String)

object Client {
  implicit val formatClient = Json.format[Client]
}

sealed abstract class CheckCase


case class OwnerCheckCase(checkCaseSubmissionId: String,
                          checkCaseReference: String,
                          checkCaseStatus: String,
                          address: String,
                          uarn: Long,
                          createdDateTime: LocalDateTime,
                          settledDate: Option[LocalDate],
                          agent: Option[Agent],
                          organisationId: Long)
                          extends CheckCase

object OwnerCheckCase {
  implicit val formatOwnerCheckCase = Json.format[OwnerCheckCase]
  implicit val formatCheckStatus = Json.format[CheckStatus]
}

case class AgentCheckCase(checkCaseSubmissionId: String,
                          checkCaseReference: String,
                          checkCaseStatus: String,
                          address: String,
                          uarn: Long,
                          createdDateTime: LocalDateTime,
                          settledDate: Option[LocalDate],
                          client: Client,
                          organisationId: Long)
                          extends CheckCase

object AgentCheckCase {
  implicit val formatCheckStatus = Json.format[CheckStatus]
  implicit val formatAgentCheckCase = Json.format[AgentCheckCase]
}

