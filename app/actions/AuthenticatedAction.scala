/*
 * Copyright 2018 HM Revenue & Customs
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
import config.Global
import connectors._
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._
import services.email.EmailService
import services.{EnrolmentResult, EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority

class AuthenticatedAction @Inject()(provider: GovernmentGatewayProvider,
                                    businessRatesAuthorisation: BusinessRatesAuthorisation,
                                    authImpl: AuthImpl,
                                    addressesConnector: Addresses,
                                    val authConnector: AuthConnector) {

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def apply(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthorisation.authenticate flatMap {
      res => handleResult(res, body)
    } recoverWith {
      case e: BadRequestException =>
        Global.onBadRequest(request, e.message)
      case _: NotFoundException =>
        Global.onHandlerNotFound(request)
      //need to catch unhandled exceptions here to propagate the request ID into the internal server error page
      case e =>
        Global.onError(request, e)
    }
  }

  def asAgent(body: AgentRequest[AnyContent] => Future[Result])(implicit messages: Messages) = apply { implicit request =>
    if (request.organisationAccount.isAgent) {
      body(AgentRequest(request.organisationAccount, request.individualAccount, request.organisationAccount.agentCode, request))
    } else {
      Future.successful(Unauthorized("Agent account required"))
    }
  }

  def toViewAssessment(authorisationId: Long, assessmentRef: Long)(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = {
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

  def toViewAssessmentsFor(authorisationId: Long)(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthorisation.authorise(authorisationId) flatMap {
      res => handleResult(res, body)
    } recover {
      case e =>
        Logger.error(e.getMessage, e)
        InternalServerError(Global.internalServerErrorTemplate(request))
    }
  }

  private def handleResult(result: AuthorisationResult, body: BasicAuthenticatedRequest[AnyContent] => Future[Result])
                          (implicit request: Request[AnyContent]) = {
    result match {
      case Authenticated(accounts)  => authImpl.success(accounts, body)
      case InvalidGGSession         => provider.redirectToLogin
      case NoVOARecord              => authImpl.noVoaRecord
      case IncorrectTrustId         => Future.successful(Unauthorized("Trust ID does not match"))
      case NonOrganisationAccount   => authImpl.noOrgAccount
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



trait AuthImpl {

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def success(accounts: Accounts, body: BasicAuthenticatedRequest[AnyContent] => Future[Result])(implicit request: Request[AnyContent]): Future[Result]

  def noVoaRecord: Future[Result]

  def noOrgAccount: Future[Result]
}

class NonEnrolmentAuth extends AuthImpl {
  override def success(
                        accounts: Accounts,
                        body: (BasicAuthenticatedRequest[AnyContent]) => Future[Result])
                      (implicit request: Request[AnyContent]): Future[Result] =
    body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))

  override def noVoaRecord: Future[Result] =
    Future.successful(Redirect(controllers.routes.CreateIndividualAccount.show))

  override def noOrgAccount: Future[Result] =
    Future.successful(Redirect(controllers.routes.Application.invalidAccountType))

}

class EnrolmentAuth @Inject()(
                               provider: GovernmentGatewayProvider,
                               enrolments: EnrolmentService,
                               emailService: EmailService,
                               val authConnector: AuthConnector,
                               auth: VPLAuthConnector
                             ) extends AuthorisedFunctions with AuthImpl {
  override def success(
                        accounts: Accounts,
                        body: BasicAuthenticatedRequest[AnyContent] => Future[Result])
                      (implicit request: Request[AnyContent]): Future[Result] = {
    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: InsufficientEnrolments =>
        enrolments
        .enrol(accounts.person.individualId, accounts.organisation.addressId).flatMap(enrolmentResult(accounts, body))
      case _: NoActiveSession => provider.redirectToLogin
      case otherException =>
        Logger.debug(s"exception thrown on authorization with message : ${otherException.getMessage}")
        throw otherException
    }

    val retrieve: Retrieval[~[~[Option[String], Option[String]], Option[String]]] = Retrievals.email and Retrievals.postCode and Retrievals.groupIdentifier
    authorised(AuthProviders(AuthProvider.GovernmentGateway) and Enrolment("HMRC-VOA-CCA")).retrieve(retrieve) {
      case email ~ postcode ~ groupId => body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
    }.recoverWith(handleError)
  }

  override def noVoaRecord: Future[Result] =
    Future.successful(Redirect(controllers.enrolment.routes.CreateEnrolmentUser.show()))


  override def noOrgAccount: Future[Result] =
      Future.successful(Redirect(controllers.enrolment.routes.CreateEnrolmentUser.show()))

  private def enrolmentResult(
                         accounts: Accounts,
                         body: BasicAuthenticatedRequest[AnyContent] => Future[Result])
                       (result: EnrolmentResult)(implicit request: Request[AnyContent]): Future[Result] = result match {
    case Success =>
      for {
      userDetails <- auth.getUserDetails
      _           <- emailService.sendMigrationEnrolmentSuccess(userDetails.userInfo.email, accounts.person.individualId, s"${accounts.person.details.firstName} ${accounts.person.details.lastName}")
      } yield Ok(views.html.createAccount.migration_success(s"PersonID: ${accounts.person.individualId}"))
    case Failure =>
      Logger.warn("Failed to enrol existing VOA user")
      body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
  }
}