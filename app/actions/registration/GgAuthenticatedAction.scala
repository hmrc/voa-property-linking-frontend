/*
 * Copyright 2021 HM Revenue & Customs
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

package actions.registration

import actions.registration.requests.RequestWithUserDetails
import auth.GovernmentGatewayProvider
import config.ApplicationConfig
import javax.inject.Inject
import models.registration.UserDetails
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Ok
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession

import scala.concurrent.{ExecutionContext, Future}

class GgAuthenticatedAction @Inject()(
      override val messagesApi: MessagesApi,
      provider: GovernmentGatewayProvider,
      override val authConnector: AuthConnector
)(
      implicit override val executionContext: ExecutionContext,
      controllerComponents: ControllerComponents,
      config: ApplicationConfig
) extends ActionBuilder[RequestWithUserDetails, AnyContent] with AuthorisedFunctions with I18nSupport {

  val logger = Logger(this.getClass.getName)

  override val parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override def invokeBlock[A](
        request: Request[A],
        block: RequestWithUserDetails[A] => Future[Result]): Future[Result] = {
    implicit val req: Request[A] = request
    implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, Some(request.session))
    logger.debug("the request called invoke block")

    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: NoActiveSession =>
        provider.redirectToLogin
      case unsupportedAffinityGroup: UnsupportedAffinityGroup =>
        logger.warn("invalid account type:", unsupportedAffinityGroup)
        Future.successful(Ok(views.html.errors.invalidAccountType()))
      case otherException: Throwable =>
        Logger.debug(s"Exception thrown on authorisation with message:", otherException)
        throw otherException
    }

    val retrieval = name and email and postCode and groupIdentifier and externalId and affinityGroup and credentialRole
    authorised(AuthProviders(GovernmentGateway) and (Organisation or Individual))
      .retrieve(retrieval) {
        case optName ~ optEmail ~ optPostCode ~ Some(groupIdentifier) ~ Some(externalId) ~ Some(affinityGroup) ~ Some(
              role) =>
          block(
            new RequestWithUserDetails(
              UserDetails
                .fromRetrieval(optName, optEmail, optPostCode, groupIdentifier, externalId, affinityGroup, role),
              request))
      }
      .recoverWith(handleError)
  }

}
