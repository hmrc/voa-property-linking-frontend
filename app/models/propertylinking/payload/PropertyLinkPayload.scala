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

package models.propertylinking.payload

import java.time.Instant

import models.propertylinking.requests.PropertyLinkRequest
import models.{Capacity, FileInfo, LinkBasis}
import play.api.libs.json.Json

case class PropertyLinkPayload(
      uarn: Long,
      organisationId: Long,
      individualId: Long,
      capacityDeclaration: Capacity,
      linkedDate: Instant,
      linkBasis: LinkBasis,
      fileInfo: List[FileInfo],
      submissionId: String
)

object PropertyLinkPayload {
  implicit val format = Json.format[PropertyLinkPayload]

  def apply(request: PropertyLinkRequest): PropertyLinkPayload =
    PropertyLinkPayload(
      uarn = request.uarn,
      organisationId = request.organisationId,
      individualId = request.individualId,
      capacityDeclaration = request.capacityDeclaration,
      linkedDate = request.linkedDate,
      linkBasis = request.linkBasis,
      fileInfo = request.fileInfo,
      submissionId = request.submissionId
    )
}
