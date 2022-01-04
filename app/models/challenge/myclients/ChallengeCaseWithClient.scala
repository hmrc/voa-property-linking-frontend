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

package models.challenge.myclients

import java.time.{LocalDate, LocalDateTime}

import models.challenge.ChallengeCaseStatus.ChallengeCaseStatus
import models.dvr.cases.check.common.Client
import play.api.libs.json.{Json, OFormat}

case class ChallengeCaseWithClient(
      challengeCaseSubmissionId: String,
      challengeCaseReference: String,
      challengeCaseStatus: ChallengeCaseStatus,
      address: String,
      uarn: Long,
      createdDateTime: LocalDateTime,
      settledDate: Option[LocalDate],
      client: Client,
      submittedBy: String,
      originatingAssessmentReference: Long
)

object ChallengeCaseWithClient {
  implicit val format: OFormat[ChallengeCaseWithClient] = Json.format
}
