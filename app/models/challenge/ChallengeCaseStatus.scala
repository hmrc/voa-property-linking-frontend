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

package models.challenge

import utils.JsonUtils

object ChallengeCaseStatus extends Enumeration {
  type ChallengeCaseStatus = Value

  val PENDING = Value("PENDING")
  val RECEIVED = Value("RECEIVED") // CDB case created, ack received
  val ASSIGNED = Value("ASSIGNED")
  val MORE_INFO_NEEDED = Value("MORE_INFO_NEEDED")
  val UNDER_REVIEW = Value("UNDER_REVIEW")
  val INITIAL_RESPONSE = Value("INITIAL_RESPONSE")
  val DECISION_SENT = Value("DECISION_SENT")

  // old statuses still possibly will be returned in some cases
  val CANCELLED = Value("CANCELLED")
  val OPEN = Value("OPEN")
  val CLOSED = Value("CLOSED")

  implicit val format = JsonUtils.enumFormat(ChallengeCaseStatus)

}
