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

package models.dvr.cases.check.common

import models.Party
import play.api.libs.json.{Json, OFormat}

case class Agent(organisationId: Long, code: Long, organisationName: String)

object Agent {

  def apply(legacyParty: Party): Agent =
    Agent(
      organisationId = legacyParty.organisationId,
      code = legacyParty.agentCode,
      organisationName = legacyParty.organisationName
    )

  implicit val format: OFormat[Agent] = Json.format[Agent]
}
