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

package models.attachment

import play.api.libs.json._
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
  implicit val format: Format[AttachmentState] = new Format[AttachmentState] {
    override def reads(json: JsValue): JsResult[AttachmentState] =
      json match {
        case JsString("Received")                  => JsSuccess(Received)
        case JsString("Initiated")                 => JsSuccess(Initiated)
        case JsString("ValidationFailed")          => JsSuccess(ValidationFailed)
        case JsString("ScanPending")               => JsSuccess(ScanPending)
        case JsString("ScanReceived")              => JsSuccess(ScanReceived)
        case JsString("MetadataPending")           => JsSuccess(MetadataPending)
        case JsString("MetadataReceived")          => JsSuccess(MetadataReceived)
        case JsString("UploadPending")             => JsSuccess(UploadPending)
        case JsString("Uploading")                 => JsSuccess(Uploading)
        case JsString("UploadAttachmentFailed")    => JsSuccess(UploadAttachmentFailed)
        case JsString("UploadAttachmentComplete")  => JsSuccess(UploadAttachmentComplete)
        case JsString("UploadingScanResults")      => JsSuccess(UploadingScanResults)
        case JsString("UploadScanResultsFailed")   => JsSuccess(UploadScanResultsFailed)
        case JsString("UploadScanResultsComplete") => JsSuccess(UploadScanResultsComplete)
        case _                                     => JsError("Invalid AttachmentState")
      }

    override def writes(o: AttachmentState): JsValue =
      o match {
        case Received                  => JsString("Received")
        case Initiated                 => JsString("Initiated")
        case ValidationFailed          => JsString("ValidationFailed")
        case ScanPending               => JsString("ScanPending")
        case ScanReceived              => JsString("ScanReceived")
        case MetadataPending           => JsString("MetadataPending")
        case MetadataReceived          => JsString("MetadataReceived")
        case UploadPending             => JsString("UploadPending")
        case Uploading                 => JsString("Uploading")
        case UploadAttachmentFailed    => JsString("UploadAttachmentFailed")
        case UploadAttachmentComplete  => JsString("UploadAttachmentComplete")
        case UploadingScanResults      => JsString("UploadingScanResults")
        case UploadScanResultsFailed   => JsString("UploadScanResultsFailed")
        case UploadScanResultsComplete => JsString("UploadScanResultsComplete")
        case _                         => throw new IllegalStateException(s"Unsupported AttachmentState: ${o.getClass.getSimpleName}")
      }
  }
}
