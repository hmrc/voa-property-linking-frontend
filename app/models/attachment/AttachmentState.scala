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

  implicit val format = Jsonx.formatSealed[AttachmentState]
}
