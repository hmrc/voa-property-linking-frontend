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

import javax.inject.Inject

import auth.GovernmentGatewayProvider
import config.{ApplicationConfig, Global}
import connectors._
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.{EnrolmentResult, EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedAction @Inject()(override val messagesApi: MessagesApi,
                                    provider: GovernmentGatewayProvider,
                                    businessRatesAuthorisation: BusinessRatesAuthorisation,
                                    enrolmentService: EnrolmentService,
                                    val authConnector: AuthConnector
                                   )(implicit val messageApi: MessagesApi, config: ApplicationConfig, executionContext: ExecutionContext)
  extends ActionBuilder[BasicAuthenticatedRequest] with AuthorisedFunctions with I18nSupport{

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  override def invokeBlock[A](request: Request[A], block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    businessRatesAuthorisation.authenticate flatMap {
      res => handleResult(res, block)(request, hc)
    }
  }

  def asAgent(body: AgentRequest[AnyContent] => Future[Result])(implicit messages: Messages) = this.async { implicit request =>
    if (request.organisationAccount.isAgent) {
      body(AgentRequest(request.organisationAccount, request.individualAccount, request.organisationAccount.agentCode, request))
    } else {
      Future.successful(Unauthorized("Agent account required"))
    }
  }

  def toViewAssessment(authorisationId: Long, assessmentRef: Long)(body: BasicAuthenticatedRequest[_] => Future[Result]) = {
    Action.async { implicit request =>
      businessRatesAuthorisation.authorise(authorisationId, assessmentRef) flatMap {
        res => handleResult(res, body)
      } recover {
        case e =>
          Logger.error(e.getMessage, e)
          InternalServerError(Global.internalServerErrorTemplate(request))
      }
    }
  }

  def toViewAssessmentsFor(authorisationId: Long)(body: BasicAuthenticatedRequest[_] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthorisation.authorise(authorisationId) flatMap {
      res => handleResult(res, body)
    } recover {
      case e =>
        Logger.error(e.getMessage, e)
        InternalServerError(Global.internalServerErrorTemplate(request))
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
        Logger.debug(s"Exception thrown on authorisation with message: ${otherException.getMessage}")
        throw otherException
    }

    val retrieval = allEnrolments and credentialRole

    authorised(AuthProviders(AuthProvider.GovernmentGateway)).retrieve(retrieval) {
      case enrolments ~ role => {
        val action = body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
        isAssistant(role) match {
          case false => {
            if (config.stubEnrolment) {
              Logger.info("Enrolment stubbed")
            } else {
              enrolments.getEnrolment("HMRC-VOA-CCA") match {
                case Some(enrolment) =>
                case None =>
                  enrolmentService.enrol(accounts.person.individualId, accounts.organisation.addressId).flatMap(existingUserEnrolmentResult(accounts, body))
              }
            }
            action
          }
          case true => action
        }
      }
    }.recoverWith(handleError)
  }

  private def isAssistant(credentialRole: Option[CredentialRole])(implicit request: Request[_]): Boolean = {
    credentialRole match {
      case Some(Assistant) => true
      case _ => false
    }
  }

  def noVoaRecord: Future[Result] =
    Future.successful(Redirect(controllers.registration.routes.RegistrationController.show()))

  private def existingUserEnrolmentResult[A](
                                           accounts: Accounts,
                                           body: BasicAuthenticatedRequest[A] => Future[Result]
                                         )(implicit request: Request[A], hc: HeaderCarrier): PartialFunction[EnrolmentResult, Future[Result]] = {
    case Success =>
      Logger.info("Existing VOA user successfully enrolled")
      body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
    case Failure =>
      Logger.warn("Failed to enrol existing VOA user")
      body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
  }

  private def handleResult[A](result: AuthorisationResult, body: BasicAuthenticatedRequest[A] => Future[Result])
                             (implicit request: Request[A], hc: HeaderCarrier) = {
    result match {
      case Authenticated(accounts)  => success(accounts, body)(request, hc)
      case InvalidGGSession         => provider.redirectToLogin
      case NoVOARecord              => noVoaRecord
      case IncorrectTrustId         => Future.successful(Unauthorized("Trust ID does not match"))
      case InvalidAccountType       => Future.successful(Redirect(controllers.routes.Application.invalidAccountType()))
      case ForbiddenResponse        => Future.successful(Forbidden(views.html.errors.forbidden()))
      case NonGroupIDAccount        => Future.successful(Redirect(controllers.routes.Application.invalidAccountType()))
    }
  }
}

sealed trait AuthenticatedRequest[A] extends Request[A] {
  val organisationAccount: GroupAccount
  val individualAccount: DetailedIndividualAccount

  def organisationId: Long = organisationAccount.id

  def personId: Long = individualAccount.individualId
}

case class BasicAuthenticatedRequest[A](organisationAccount: GroupAccount, individualAccount: DetailedIndividualAccount, request: Request[A])
  extends WrappedRequest[A](request) with AuthenticatedRequest[A]

case class AgentRequest[A](organisationAccount: GroupAccount, individualAccount: DetailedIndividualAccount, agentCode: Long, request: Request[A])
  extends WrappedRequest[A](request) with AuthenticatedRequest[A]
