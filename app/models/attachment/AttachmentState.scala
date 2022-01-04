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

import ai.x.play.json.Jsonx
sealed trait AttachmentState extends Product with Serializable

case object Received extends AttachmentState
case object Initiated extends AttachmentState
case object ValidationFailed extends AttachmentState
case object ScanPending extends AttachmentState
case object ScanReceived extends AttachmentState
case object MetadataPending extends AttachmentState
case object MetadataReceived extends AttachmentState
case object UploadPending extends AttachmentState
case object Uploading extends AttachmentState
case object UploadAttachmentFailed extends AttachmentState
case object UploadAttachmentComplete extends AttachmentState
case object UploadingScanResults extends AttachmentState
case object UploadScanResultsFailed extends AttachmentState
case object UploadScanResultsComplete extends AttachmentState

object AttachmentState {
  import ai.x.play.json.SingletonEncoder.simpleName
  import ai.x.play.json.implicits.formatSingleton
  implicit val format = Jsonx.formatSealed[AttachmentState]
}
