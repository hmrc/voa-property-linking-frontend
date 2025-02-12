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

package models

import binders.propertylinks.ClaimPropertyReturnToPage.ClaimPropertyReturnToPage
import models.attachment.Attachment
import models.upscan.PreparedUpload
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class LinkingSession(
      address: String,
      uarn: Long,
      submissionId: String,
      personId: Long,
      earliestStartDate: LocalDate,
      propertyRelationship: Option[PropertyRelationship],
      propertyOwnership: Option[PropertyOwnership],
      propertyOccupancy: Option[PropertyOccupancy],
      hasRatesBill: Option[Boolean],
      uploadEvidenceData: UploadEvidenceData = UploadEvidenceData.empty,
      fileReference: Option[String] = None,
      fileReferenceNew: Option[String] = None,
      evidenceType: Option[EvidenceType] = None,
      clientDetails: Option[ClientDetails] = None,
      localAuthorityReference: String,
      rtp: ClaimPropertyReturnToPage,
      valuationId: Option[Long] = None,
      fromCya: Option[Boolean] = Some(false),
      occupierEvidenceType: Option[EvidenceType] = None,
      isSubmitted: Option[Boolean] = Some(false),
      preparedUpload: Option[PreparedUpload] = None,
      attachment: Option[Attachment] = None
)

object LinkingSession {
  implicit val format: OFormat[LinkingSession] = Json.format[LinkingSession]

}
