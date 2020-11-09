/*
 * Copyright 2020 HM Revenue & Customs
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

import models.ListType.ListType
import play.api.libs.json.{Format, Json}

case class PropertyLink(authorisationId: Long, submissionId: String, uarn: Long, address: String, agents: Seq[Party])

object PropertyLink {
  implicit val format: Format[PropertyLink] = Json.format[PropertyLink]
}

case class ApiAssessments(
      authorisationId: Long,
      submissionId: String,
      uarn: Long,
      address: String,
      pending: Boolean,
      capacity: Option[String],
      assessments: Seq[ApiAssessment],
      agents: Seq[Party]
)

object ApiAssessments {
  implicit val format: Format[ApiAssessments] = Json.format[ApiAssessments]
}

case class ApiAssessment(
      authorisationId: Long,
      assessmentRef: Long,
      listYear: String,
      uarn: Long,
      effectiveDate: Option[LocalDate],
      rateableValue: Option[Long],
      address: PropertyAddress,
      billingAuthorityReference: String,
      listType: ListType,
      currentFromDate: Option[LocalDate] = None,
      currentToDate: Option[LocalDate] = None
)

object ApiAssessment {
  implicit val format: Format[ApiAssessment] = Json.format[ApiAssessment]
}

case class PropertyLinkResponse(resultCount: Option[Long], propertyLinks: Seq[PropertyLink])

object PropertyLinkResponse {
  implicit val format: Format[PropertyLinkResponse] = Json.format[PropertyLinkResponse]
}
