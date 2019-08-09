package models.upscan

import utils.JsonUtils

object FailureReason extends Enumeration {
  type FailureReason = Value

  val QUARANTINED = Value("QUARANTINE")
  val REJECTED = Value("REJECTED")
  val UNKNOWN = Value("UNKNOWN")

  implicit val format = JsonUtils.enumFormat(FailureReason)
}
