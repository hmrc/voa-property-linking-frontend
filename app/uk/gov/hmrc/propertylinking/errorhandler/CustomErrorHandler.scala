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

package uk.gov.hmrc.propertylinking.errorhandler

import config.ApplicationConfig
import connectors.authorisation.errorhandler.exceptions.AuthorisationFailure
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.{Forbidden, NotFound, Redirect}
import play.api.mvc.{RequestHeader, Result}
import play.mvc.Http.Status.{FORBIDDEN, NOT_FOUND}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderNames, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import utils.DateTimeUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomErrorHandler @Inject()(
      errorView: views.html.errors.error,
      forbiddenView: views.html.errors.forbidden,
      technicalDifficultiesView: views.html.errors.technicalDifficulties,
      notFoundView: views.html.errors.notFound,
      alreadySubmittedView: views.html.errors.alreadySubmitted,
      dateTime: DateTimeUtil)(
      implicit override val messagesApi: MessagesApi,
      appConfig: ApplicationConfig,
      implicit val ec: ExecutionContext)
    extends FrontendErrorHandler with Logging with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
        implicit request: RequestHeader): Future[Html] =
    Future.successful(errorView(pageTitle, heading, message))

  override def internalServerErrorTemplate(implicit request: RequestHeader): Future[Html] =
    Future.successful(technicalDifficultiesView(extractErrorReference(request), dateTime.getDateTime.toLocalDateTime))

  def forbiddenErrorTemplate(implicit request: RequestHeader): Html = {
    val messages: Messages = messagesApi.preferred(request)
    forbiddenView()(request, messages, appConfig)
  }
  def notFoundErrorTemplate(implicit request: RequestHeader): Html = {
    val messages: Messages = messagesApi.preferred(request)
    notFoundView()(request, messages, appConfig)
  }
  def alreadySubmittedTemplate(implicit request: RequestHeader): Html = {
    val messages: Messages = messagesApi.preferred(request)
    alreadySubmittedView()(request, messages, appConfig)
  }

  private def extractErrorReference(request: RequestHeader): Option[String] = {
    val requestId = request.headers.get(HeaderNames.xRequestId)
    if (requestId.exists(_.contains("govuk-tax-"))) {
      // old xRequestId format with gov-uk-tax prefix
      requestId.map(_.split("govuk-tax-")(1))
    } else {
      //new xRequestId format
      requestId
    }
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    statusCode match {
      case FORBIDDEN => Future.successful(Forbidden(forbiddenErrorTemplate(request)))
      case NOT_FOUND => Future.successful(NotFound(notFoundErrorTemplate(request)))
      case _         => super.onClientError(request, statusCode, message)
    }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    exception match {
      case error: AuthorisationFailure =>
        logger.info(s"business rates authorisation returned ${error.message}, redirecting to login.")
        Future.successful(
          Redirect(appConfig.basGatewaySignInUrl, Map("continue_url" -> Seq(request.uri), "origin" -> Seq("voa"))))
      case upstreamEx: UpstreamErrorResponse if upstreamEx.statusCode == FORBIDDEN =>
        Future.successful(Forbidden(forbiddenErrorTemplate(request)))
      case _ =>
        super.onServerError(request, exception)
    }
}
