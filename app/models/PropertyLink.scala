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

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class PropertyLink(
                         authorisationId: Long,
                         submissionId: String,
                         uarn: Long,
                         organisationId: Long,
                         address: String,
                         capacityDeclaration: Capacity,
                         linkedDate: LocalDate,
                         pending: Boolean,
                         assessments: Seq[Assessment],
                         agents: Seq[Party])

object PropertyLink {
  implicit val format = Json.format[PropertyLink]
}

case class PropertyLinkResponse(resultCount: Option[Long],
                                propertyLinks: Seq[PropertyLink])

object PropertyLinkResponse {
  implicit val format: Format[PropertyLinkResponse] = Json.format[PropertyLinkResponse]
}