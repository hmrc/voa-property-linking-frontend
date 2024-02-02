/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, single}
import play.api.mvc.PathBindable
import utils.JsonUtils

import scala.util.Try

object FileStatus extends Enumeration {
  type FileStatus = Value

  val READY = Value("READY")
  val UPLOADING = Value("UPLOADING")
  val FAILED = Value("FAILED")

  implicit val format = JsonUtils.enumFormat(FileStatus)

  implicit object Binder extends PathBindable[FileStatus] {

    override def bind(key: String, value: String): Either[String, FileStatus] =
      Try(FileStatus.withName(value)).toOption
        .map(Right.apply)
        .getOrElse(Left(s"Invalid value for FileStatus: $value"))

    override def unbind(key: String, value: FileStatus): String = value.toString
  }

  val fileStatusForm = Form(single("fileStatus" -> nonEmptyText))

}
