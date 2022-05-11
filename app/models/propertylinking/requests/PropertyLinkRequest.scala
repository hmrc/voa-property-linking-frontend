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

package models.propertylinking.requests

import java.time.Instant

import models.{Capacity, FileInfo, LinkBasis, LinkingSession}
import play.api.libs.json.Json

case class PropertyLinkRequest(
      uarn: Long,
      organisationId: Long,
      individualId: Long,
      capacityDeclaration: Capacity,
      linkedDate: Instant,
      linkBasis: LinkBasis,
      fileInfo: List[FileInfo],
      references: List[String],
      submissionId: String,
      clientId: Option[Long] = None
)

object PropertyLinkRequest {
  implicit val format = Json.format[PropertyLinkRequest]

  def apply(session: LinkingSession, organisationId: Long): PropertyLinkRequest =
    PropertyLinkRequest(
      uarn = session.uarn,
      organisationId = organisationId,
      individualId = session.personId,
      capacityDeclaration = Capacity(session),
      linkedDate = Instant.now,
      linkBasis = session.uploadEvidenceData.linkBasis,
      fileInfo = session.uploadEvidenceData.fileInfo.toList,
      references = session.uploadEvidenceData.attachments.toList.flatMap(_.keys),
      submissionId = session.submissionId,
      clientId = session.clientDetails.map(_.organisationId)
    )
}
