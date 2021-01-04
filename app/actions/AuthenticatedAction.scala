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

package actions

import actions.requests.{AgentRequest, BasicAuthenticatedRequest}
import auth.GovernmentGatewayProvider
import config.ApplicationConfig
import connectors.authorisation._
import javax.inject.Inject
import models.Accounts
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.{EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedAction @Inject()(
      override val messagesApi: MessagesApi,
      provider: GovernmentGatewayProvider,
      businessRatesAuthorisation: BusinessRatesAuthorisationConnector,
      enrolmentService: EnrolmentService,
      override val authConnector: AuthConnector)(
      implicit controllerComponents: MessagesControllerComponents,
      config: ApplicationConfig,
      override val executionContext: ExecutionContext
) extends ActionBuilder[BasicAuthenticatedRequest, AnyContent] with AuthorisedFunctions with I18nSupport {

  val logger = Logger(this.getClass.getName)

  override val parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter
      .fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))

  override def invokeBlock[A](
        request: Request[A],
        block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] =
    businessRatesAuthorisation.authenticate(hc(request)).flatMap { res =>
      handleResult(res, block)(request, hc(request))
    }

  def asAgent(body: AgentRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    this.async { implicit request =>
      request.organisationAccount.agentCode
        .map(code => body(AgentRequest(request.organisationAccount, request.individualAccount, code, request)))
        .getOrElse(Future.successful(
          Unauthorized(views.html.errors.error("Bad Request", "Bad Request", "Agent account required"))))
    }

  def success[A](accounts: Accounts, body: BasicAuthenticatedRequest[A] => Future[Result])(
        implicit request: Request[A],
        hc: HeaderCarrier): Future[Result] = {
    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: InsufficientEnrolments =>
        logger.info("CCA account holder with insufficent enrolments, Migrating")
        enrolmentService.enrol(accounts.person.individualId, accounts.organisation.addressId).flatMap {
          case Success =>
            Logger.info("Existing VOA user successfully enrolled")
            body(
              BasicAuthenticatedRequest(
                organisationAccount = accounts.organisation,
                individualAccount = accounts.person,
                request = request))
          case Failure =>
            Logger.warn("Failed to enrol existing VOA user")
            body(
              BasicAuthenticatedRequest(
                organisationAccount = accounts.organisation,
                individualAccount = accounts.person,
                request = request))
        }
      case ex: UnsupportedCredentialRole => //This case should not happen.
        logger.warn(
          s"unsupported credential role on existing VOA account, with message ${ex.msg}, for reason ${ex.reason}",
          ex)
        Future.successful(Ok(views.html.errors.invalidAccountType()))
      case _: UnsupportedAffinityGroup =>
        logger.warn("invalid account type already has a CCA account")
        Future.successful(Ok(views.html.errors.invalidAccountType()))
      case _: NoActiveSession =>
        provider.redirectToLogin
      case otherException =>
        Logger.warn(s"Exception thrown on authorisation with message:", otherException)
        throw otherException
    }

    authorised(AuthProviders(GovernmentGateway) and (Organisation or Individual) and (Assistant or (Enrolment(
      "HMRC-VOA-CCA") and User))) {
      body(
        BasicAuthenticatedRequest(
          organisationAccount = accounts.organisation,
          individualAccount = accounts.person,
          request = request))
    }.recoverWith(handleError)
  }

  private def handleResult[A](result: AuthorisationResult, body: BasicAuthenticatedRequest[A] => Future[Result])(
        implicit request: Request[A],
        hc: HeaderCarrier) = {
    import AuthorisationResult._
    result match {
      case Authenticated(accounts) => success(accounts, body)(request, hc)
      case InvalidGGSession        => provider.redirectToLogin
      case NoVOARecord             => Future.successful(Redirect(controllers.registration.routes.RegistrationController.show()))
      case IncorrectTrustId        => Future.successful(Unauthorized("Trust ID does not match"))
      case InvalidAccountType      => Future.successful(Redirect(controllers.routes.Application.invalidAccountType()))
      case ForbiddenResponse       => Future.successful(Forbidden(views.html.errors.forbidden()))
      case NonGroupIDAccount       => Future.successful(Redirect(controllers.routes.Application.invalidAccountType()))
    }
  }
}
