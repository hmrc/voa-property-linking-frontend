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

package models

import play.api.libs.json.Json
case class PropertyLinkIds(caseCreator: CaseCreator, interestedParty: InterestedParty)
object PropertyLinkIds {
  implicit val formatInterestedParty = Json.format[InterestedParty]
  implicit val formatCaseCreator = Json.format[CaseCreator]
  implicit val format = Json.format[PropertyLinkIds]
}

case class InterestedParty(personId: Long, organisationId: Long)
case class CaseCreator(personId: Long, organisationId: Long)
