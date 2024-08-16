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

package models.challenge

import play.api.libs.json.Format
import utils.JsonUtils

object ChallengeCaseStatus extends Enumeration {
  type ChallengeCaseStatus = Value

  val PENDING: ChallengeCaseStatus.Value = Value("PENDING")
  val RECEIVED: ChallengeCaseStatus.Value = Value("RECEIVED") // CDB case created, ack received
  val ASSIGNED: ChallengeCaseStatus.Value = Value("ASSIGNED")
  val MORE_INFO_NEEDED: ChallengeCaseStatus.Value = Value("MORE_INFO_NEEDED")
  val UNDER_REVIEW: ChallengeCaseStatus.Value = Value("UNDER_REVIEW")
  val INITIAL_RESPONSE: ChallengeCaseStatus.Value = Value("INITIAL_RESPONSE")
  val DECISION_SENT: ChallengeCaseStatus.Value = Value("DECISION_SENT")

  // old statuses still possibly will be returned in some cases
  val CANCELLED: ChallengeCaseStatus.Value = Value("CANCELLED")
  val OPEN: ChallengeCaseStatus.Value = Value("OPEN")
  val CLOSED: ChallengeCaseStatus.Value = Value("CLOSED")

  implicit val format: Format[ChallengeCaseStatus.Value] = JsonUtils.enumFormat(ChallengeCaseStatus)

}
