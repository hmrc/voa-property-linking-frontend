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

package controllers.propertyLinking

import java.util.UUID

import auditing.AuditingService
import config.ApplicationConfig
import controllers._
import session.{LinkingSessionRequest, WithLinkingSession}

trait FileUploadHelpers {
  self: PropertyLinkingController =>

  val config: ApplicationConfig
  val withLinkingSession: WithLinkingSession
  lazy val fileUploadBaseUrl = config.fileUploadUrl
  val successUrl: String

  def fileUploaded() = withLinkingSession { implicit request =>
    val envelopeId = request.ses.envelopeId
    AuditingService.sendEvent("file upload success", Map("envelopeId" -> envelopeId))
    Redirect(propertyLinking.routes.Declaration.show())
  }

  def fileUploadUrl(failureUrl: String)(implicit request: LinkingSessionRequest[_]): String = {
    val envelopeId = request.ses.envelopeId
    val fileId = UUID.randomUUID().toString
    val absoluteSuccessUrl = s"${config.serviceUrl}$successUrl"
    val absoluteFailureUrl = s"${config.serviceUrl}$failureUrl"

    s"$fileUploadBaseUrl/upload/envelopes/$envelopeId/files/$fileId?redirect-success-url=$absoluteSuccessUrl&redirect-error-url=$absoluteFailureUrl"
  }
}

sealed trait FileUploadResult

case object FileAccepted extends FileUploadResult

case object FileMissing extends FileUploadResult

case object FileTooLarge extends FileUploadResult

case object InvalidFileType extends FileUploadResult