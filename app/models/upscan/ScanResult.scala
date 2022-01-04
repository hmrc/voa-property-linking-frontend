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

package models.upscan

import java.time.Instant

import models.upscan.FailureReason.FailureReason
import models.upscan.FileStatus.FileStatus
import play.api.libs.json._

case class ScanResult(
      reference: Reference,
      fileStatus: FileStatus,
      downloadUrl: Option[String],
      uploadDetails: Option[UploadDetails],
      failureDetails: Option[FailureDetails])

object ScanResult {
  implicit val format: OFormat[ScanResult] = Json.format
  object ScanFailed {
    def unapply(scanResult: ScanResult): Option[FailureDetails] =
      scanResult.failureDetails
  }
  object ScanSucceeded {
    def unapply(scanResult: ScanResult): Option[String] =
      scanResult.downloadUrl
  }
}

case class UploadDetails(uploadTimestamp: Instant, checksum: String)

object UploadDetails {
  implicit val format: OFormat[UploadDetails] = Json.format
}

case class FailureDetails(failureReason: FailureReason, message: String)

object FailureDetails {
  implicit val format: OFormat[FailureDetails] = Json.format
}
