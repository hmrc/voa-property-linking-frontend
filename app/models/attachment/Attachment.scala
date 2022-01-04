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

package models.attachment

import java.time.Instant
import java.util.UUID

import auth.Principal
import models.upscan.{PreparedUpload, ScanResult}
import play.api.libs.json.{Json, OFormat}

case class Attachment(
      _id: UUID,
      initiatedAt: Instant,
      fileName: String,
      mimeType: String,
      destination: String,
      data: Map[String, String],
      state: AttachmentState,
      history: List[HistoryItem],
      scanResult: Option[ScanResult],
      initiateResult: Option[PreparedUpload],
      principal: Principal)

object Attachment {
  implicit val format: OFormat[Attachment] = Json.format
}
