/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.voa.propertylinking.errorhandler

import java.time.{Instant, LocalDateTime, ZoneId}

import auth.GovernmentGatewayProvider
import config.ApplicationConfig
import connectors.authorisation.errorhandler.exceptions.BraAuthorisationFailure
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

import scala.concurrent.Future

class CustomErrorHandler @Inject()(
      provider: GovernmentGatewayProvider
)(implicit override val messagesApi: MessagesApi, appConfig: ApplicationConfig)
    extends FrontendErrorHandler with I18nSupport {

  val logger: Logger = Logger(this.getClass)

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
        implicit request: Request[_]): Html =
    views.html.errors.error(pageTitle, heading, message)

  override def internalServerErrorTemplate(implicit request: Request[_]): Html =
    views.html.errors.technicalDifficulties(extractErrorReference(request), getDateTime)

  private def getDateTime: LocalDateTime = {
    val instant = Instant.ofEpochMilli(System.currentTimeMillis)
    LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London"))
  }

  private def extractErrorReference(request: Request[_]): Option[String] =
    request.headers.get(HeaderNames.xRequestId) map {
      _.split("-")(2)
    }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    exception match {
      case error: BraAuthorisationFailure =>
        logger.info(s"business rates authorisation returned ${error.message}, redirecting to login.")
        Future.successful(
          Redirect(
            appConfig.ggSignInUrl,
            Map("continue" -> Seq(appConfig.baseUrl + request.uri), "origin" -> Seq("voa"))))
      case _ =>
        super.onServerError(request, exception)

    }
}
