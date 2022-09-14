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

package models.dvr.cases.check.projection

import java.time.{LocalDate, LocalDateTime}

import models.challenge.ChallengeCaseStatus
import models.challenge.myclients.ChallengeCaseWithClient
import models.challenge.myorganisations.ChallengeCaseWithAgent
import models.dvr.cases.check.CheckCaseStatus
import models.dvr.cases.check.common.Agent
import models.dvr.cases.check.myclients.CheckCaseWithClient
import models.dvr.cases.check.myorganisation.CheckCaseWithAgent
import play.api.libs.json.{Json, OFormat}

case class CaseDetails(
      submittedDate: LocalDateTime,
      status: String,
      caseReference: String,
      closedDate: Option[LocalDate],
      clientOrAgent: String,
      submittedBy: String,
      agent: Option[Agent] = None
) {
  val openStatuses = Seq(
    CheckCaseStatus.OPEN,
    CheckCaseStatus.ASSIGNED,
    CheckCaseStatus.UNDER_REVIEW,
    CheckCaseStatus.RECEIVED,
    ChallengeCaseStatus.OPEN,
    ChallengeCaseStatus.UNDER_REVIEW,
    ChallengeCaseStatus.ASSIGNED,
    ChallengeCaseStatus.RECEIVED,
    ChallengeCaseStatus.MORE_INFO_NEEDED,
    ChallengeCaseStatus.PENDING,
    ChallengeCaseStatus.INITIAL_RESPONSE
  )

  def isOpen: Boolean = openStatuses.filter(_.toString == status).nonEmpty
}

object CaseDetails {

  implicit val format: OFormat[CaseDetails] = Json.format

  def apply(check: CheckCaseWithAgent): CaseDetails =
    CaseDetails(
      submittedDate = check.createdDateTime,
      status = check.checkCaseStatus.toString,
      caseReference = check.checkCaseReference,
      closedDate = check.settledDate,
      clientOrAgent = check.agent.fold("")(_.organisationName),
      submittedBy = check.submittedBy,
      agent = check.agent
    )

  def apply(check: CheckCaseWithClient): CaseDetails =
    CaseDetails(
      submittedDate = check.createdDateTime,
      status = check.checkCaseStatus.toString,
      caseReference = check.checkCaseReference,
      closedDate = check.settledDate,
      clientOrAgent = check.client.organisationName,
      submittedBy = check.submittedBy
    )

  def apply(challenge: ChallengeCaseWithAgent): CaseDetails =
    CaseDetails(
      submittedDate = challenge.createdDateTime,
      status = challenge.challengeCaseStatus.toString,
      caseReference = challenge.challengeCaseReference,
      closedDate = challenge.settledDate,
      clientOrAgent = challenge.agent.fold("")(_.organisationName),
      submittedBy = challenge.submittedBy,
      agent = challenge.agent
    )

  def apply(challenge: ChallengeCaseWithClient): CaseDetails =
    CaseDetails(
      submittedDate = challenge.createdDateTime,
      status = challenge.challengeCaseStatus.toString,
      caseReference = challenge.challengeCaseReference,
      closedDate = challenge.settledDate,
      clientOrAgent = challenge.client.organisationName,
      submittedBy = challenge.submittedBy
    )
}
