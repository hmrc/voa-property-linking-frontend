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

package actions

import auth.GovernmentGatewayProvider
import config.ApplicationConfig
import connectors.authorisation._
import connectors.authorisation._
import javax.inject.Inject
import models.Accounts
import models.registration.UserDetails
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.{EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedAction @Inject()(override val messagesApi: MessagesApi,
                                    provider: GovernmentGatewayProvider,
                                    businessRatesAuthorisation: BusinessRatesAuthorisation,
                                    enrolmentService: EnrolmentService,
                                    override val authConnector: AuthConnector
                                   )(implicit val messageApi: MessagesApi, config: ApplicationConfig, executionContext: ExecutionContext)
  extends ActionBuilder[BasicAuthenticatedRequest] with AuthorisedFunctions with I18nSupport {

  val logger = Logger(this.getClass.getName)

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSessionAndRequest(request.headers, Some(request.session), request = Some(request))

  override def invokeBlock[A](request: Request[A], block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    logger.debug("the request called invoke block")
    businessRatesAuthorisation.authenticate(hc(request)).flatMap {
      res =>
        logger.debug("the request passed through business-rates-authorisation")
        handleResult(res, block)(request, hc(request))
    }
  }

  def asAgent(body: AgentRequest[AnyContent] => Future[Result])(implicit messages: Messages): Action[AnyContent] =
    this.async { implicit request =>
      if (request.organisationAccount.isAgent) {
        body(new AgentRequest(request.organisationAccount, request.individualAccount, request.organisationAccount.agentCode, request))
      } else {
        Future.successful(Unauthorized("Agent account required"))
      }
    }

  def success[A](
                  accounts: Accounts,
                  body: BasicAuthenticatedRequest[A] => Future[Result])
                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: NoActiveSession =>
        provider.redirectToLogin
      case otherException =>
        Logger.debug(s"Exception thrown on authorisation with message:", otherException)
        throw otherException
    }

    val retrieval = allEnrolments and name and email and postCode and groupIdentifier and externalId and affinityGroup and credentialRole
    authorised(AuthProviders(GovernmentGateway)).retrieve(retrieval) {
      case enrolments ~ name ~ optEmail ~ optPostCode ~ Some(groupIdentifier) ~ Some(externalId) ~ Some(affinityGroup) ~ Some(role) =>
        if (role != Assistant) {
          if (config.stubEnrolment) {
            Logger.info("Enrolment stubbed")
          } else {
            if (enrolments.getEnrolment("HMRC-VOA-CCA").isEmpty)
              enrolmentService.enrol(accounts.person.individualId, accounts.organisation.addressId).map {
                case Success => Logger.info("Existing VOA user successfully enrolled")
                case Failure => Logger.warn("Failed to enrol existing VOA user")
              }
          }
        }
        body(new BasicAuthenticatedRequest(
          organisationAccount = accounts.organisation,
          individualAccount = accounts.person,
          userDetails = UserDetails.fromRetrieval(name, optEmail, optPostCode, groupIdentifier, externalId, affinityGroup, role),
          request = request))
    }.recoverWith(handleError)
  }

  private def handleResult[A](result: AuthorisationResult, body: BasicAuthenticatedRequest[A] => Future[Result])
                             (implicit request: Request[A], hc: HeaderCarrier) = {
    import AuthorisationResult._
    result match {
      case Authenticated(accounts) => success(accounts, body)(request, hc)
      case InvalidGGSession => provider.redirectToLogin
      case NoVOARecord => Future.successful(Redirect(controllers.registration.routes.RegistrationController.show()))
      case IncorrectTrustId => Future.successful(Unauthorized("Trust ID does not match"))
      case InvalidAccountType => Future.successful(Redirect(controllers.routes.Application.invalidAccountType()))
      case ForbiddenResponse => Future.successful(Forbidden(views.html.errors.forbidden()))
      case NonGroupIDAccount => Future.successful(Redirect(controllers.routes.Application.invalidAccountType()))
    }
  }
}
