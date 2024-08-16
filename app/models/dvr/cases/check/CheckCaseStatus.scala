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

package models.dvr.cases.check

import play.api.libs.json.Format
import utils.JsonUtils

object CheckCaseStatus extends Enumeration {

  type CheckCaseStatus = Value

  val PENDING: CheckCaseStatus.Value = Value("PENDING")
  val RECEIVED: CheckCaseStatus.Value = Value("RECEIVED")
  val ASSIGNED: CheckCaseStatus.Value = Value("ASSIGNED")
  val UNDER_REVIEW: CheckCaseStatus.Value = Value("UNDER_REVIEW")
  val DECISION_SENT: CheckCaseStatus.Value = Value("DECISION_SENT")
  val CANCELLED: CheckCaseStatus.Value = Value("CANCELLED")
  val OPEN: CheckCaseStatus.Value = Value("OPEN")
  val CLOSED: CheckCaseStatus.Value = Value("CLOSED")

  implicit val format: Format[CheckCaseStatus.Value] = JsonUtils.enumFormat(CheckCaseStatus)
}
