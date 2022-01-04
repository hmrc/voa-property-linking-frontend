/*
 * Copyright 2022 HM Revenue & Customs
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
import models.registration.UserDetails
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Ok
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequestAndSession

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GgAuthenticatedAction @Inject()(
      override val messagesApi: MessagesApi,
      provider: GovernmentGatewayProvider,
      override val authConnector: AuthConnector,
      invalidAccountTypeView: views.html.errors.invalidAccountType
)(
      implicit override val executionContext: ExecutionContext,
      controllerComponents: ControllerComponents,
      config: ApplicationConfig
) extends ActionBuilder[RequestWithUserDetails, AnyContent] with AuthorisedFunctions with I18nSupport with Logging {

  override val parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override def invokeBlock[A](
        request: Request[A],
        block: RequestWithUserDetails[A] => Future[Result]): Future[Result] = {

    implicit val req: Request[A] = request
    implicit val hc: HeaderCarrier = fromRequestAndSession(request, request.session)

    logger.debug("the request called invoke block")

    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: NoActiveSession =>
        provider.redirectToLogin
      case unsupportedAffinityGroup: UnsupportedAffinityGroup =>
        logger.warn("invalid account type:", unsupportedAffinityGroup)
        Future.successful(Ok(invalidAccountTypeView()))
      case otherException: Throwable =>
        logger.debug(s"Exception thrown on authorisation with message:", otherException)
        throw otherException
    }

    val retrieval = name and email and postCode and groupIdentifier and externalId and affinityGroup and credentialRole and confidenceLevel
    authorised(AuthProviders(GovernmentGateway) and (Organisation or Individual))
      .retrieve(retrieval) {
        case optName ~ optEmail ~ optPostCode ~ Some(groupIdentifier) ~ Some(externalId) ~ Some(affinityGroup) ~ Some(
              role) ~ confidenceLevel =>
          block(
            new RequestWithUserDetails(
              UserDetails
                .fromRetrieval(
                  name = optName,
                  optEmail = optEmail,
                  optPostCode = optPostCode,
                  groupIdentifier = groupIdentifier,
                  externalId = externalId,
                  affinityGroup = affinityGroup,
                  role = role,
                  confidenceLevel = confidenceLevel
                ),
              request
            ))
      }
      .recoverWith(handleError)
  }

}
