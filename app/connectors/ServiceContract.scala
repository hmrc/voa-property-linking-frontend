/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.Json
import serialization.JsonFormats._

case class CapacityDeclaration(capacity: CapacityType, interestedBefore2017: Boolean, fromDate: Option[LocalDate],
                               stillInterested: Boolean, toDate: Option[LocalDate] = None)

case class FileInfo(fileName: String, fileType: String)

case class PropertyLinkRequest(uarn: Long,
                               organisationId: Int,
                               individualId: Int,
                               capacityDeclaration: Capacity,
                               linkedDate: DateTime,
                               linkBasis: LinkBasis,
                               fileInfo: Seq[FileInfo],
                               submissionId: String)

object PropertyLinkRequest {
  implicit val format = Json.format[PropertyLinkRequest]
}

case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PropertyLink])

case class UpdatedRepresentation(representationId: Long, canCheck: AgentPermission, canChallenge: AgentPermission)

object UpdatedRepresentation {
  implicit val format = Json.format[UpdatedRepresentation]
}
