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

package actions

import actions.requests.StaticPageRequest
import config.ApplicationConfig
import connectors.authorisation._
import javax.inject.Inject
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class StaticPageAction @Inject()(
      override val messagesApi: MessagesApi,
      businessRatesAuthorisation: BusinessRatesAuthorisationConnector,
      override val authConnector: AuthConnector)(
      implicit controllerComponents: MessagesControllerComponents,
      config: ApplicationConfig,
      override val executionContext: ExecutionContext
) extends ActionBuilder[StaticPageRequest, AnyContent] with AuthorisedFunctions with I18nSupport with Logging {

  override val parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

  override def invokeBlock[A](request: Request[A], block: StaticPageRequest[A] => Future[Result]): Future[Result] =
    businessRatesAuthorisation.authenticate(hc(request)).flatMap { res =>
      handleResult(res, block)(request, hc(request))
    }

  private def handleResult[A](result: AuthorisationResult, body: StaticPageRequest[A] => Future[Result])(
        implicit request: Request[A],
        hc: HeaderCarrier) = {
    import AuthorisationResult._
    result match {
      case Authenticated(accounts) =>
        body(StaticPageRequest(accounts = Some(accounts), request = request))
      case _ =>
        body(StaticPageRequest(accounts = None, request = request))
    }
  }
}
