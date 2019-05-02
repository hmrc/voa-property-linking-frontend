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

package exceptionhandler

import config.ApplicationConfig
import javax.inject.Inject
import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{RequestHeader, Result, Results}
import uk.gov.hmrc.http.{NotFoundException, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.Future

class ErrorHandler @Inject()()(implicit val messagesApi: MessagesApi, applicationConfig: ApplicationConfig) extends HttpErrorHandler with Results {

  val logger = Logger(this.getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    implicit val r: RequestHeader = request
    implicit val messages: Messages = messagesApi.preferred(request)
    (statusCode, message) match {
      case (BAD_REQUEST, _)               => Future.successful(badRequest)
      case (NOT_FOUND, _)                 => Future.successful(notFound)
      case (UNAUTHORIZED | FORBIDDEN, _)  => Future.successful(forbidden)
      case _                              => Future.successful(internalServerError)
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    implicit val r: RequestHeader = request
    implicit val messages: Messages = messagesApi.preferred(request)

    logger.warn("An exception occurred processing the request", exception)
    exception match {
      case _: NotFoundException              => Future.successful(notFound)
      case Upstream4xxResponse(_, 400, _, _) => Future.successful(badRequest)
      case Upstream4xxResponse(_, 401, _, _) => Future.successful(forbidden)
      case Upstream4xxResponse(_, 403, _, _) => Future.successful(forbidden)
      case Upstream4xxResponse(_, 404, _, _) => Future.successful(notFound)
      case Upstream5xxResponse(_, 503, _)    => Future.successful(ServiceUnavailable(views.html.errors.serviceUnavailable()))
      case _                                 => Future.successful(internalServerError)
    }
  }

  def badRequest(implicit messages: Messages, request: RequestHeader): Result = BadRequest(views.html.errors.error(
    messages("global.error.badRequest400.title"),
    messages("global.error.badRequest400.heading"),
    messages("global.error.badRequest400.message")))

  def forbidden(implicit messages: Messages, request: RequestHeader): Result = Forbidden(views.html.errors.error(
    messages("global.error.forbidden.title"),
    messages("global.error.forbidden.heading"),
    messages("global.error.forbidden.message")))

  def notFound(implicit messages: Messages, request: RequestHeader): Result = NotFound(views.html.errors.error(
    messages("global.error.pageNotFound404.title"),
    messages("global.error.pageNotFound404.heading"),
    messages("global.error.pageNotFound404.message")))

  def internalServerError(implicit messages: Messages, request: RequestHeader): Result = NotFound(views.html.errors.error(
    messages("global.error.InternalServerError500.title"),
    messages("global.error.InternalServerError500.heading"),
    messages("global.error.InternalServerError500.message")))
}
