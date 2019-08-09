package models.upscan

import utils.JsonUtils

object FileStatus extends Enumeration {
  type FileStatus = Value

  val READY = Value("READY")
  val FAILED = Value("FAILED")

  implicit val format = JsonUtils.enumFormat(FileStatus)
}
