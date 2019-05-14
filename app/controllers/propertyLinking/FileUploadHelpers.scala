/*
 * Copyright 2019 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers._
import play.api.mvc.{Action, AnyContent}
import session.{LinkingSessionRequest, WithLinkingSession}

import scala.concurrent.Future

trait FileUploadHelpers {
  self: PropertyLinkingController =>

  val config: ApplicationConfig
  val withLinkingSession: WithLinkingSession
  lazy val fileUploadBaseUrl: String = config.fileUploadUrl
  val successUrl: String

  val fileUploaded: Action[AnyContent] = withLinkingSession { implicit request =>
    Future.successful(Redirect(propertyLinking.routes.Declaration.show()))
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