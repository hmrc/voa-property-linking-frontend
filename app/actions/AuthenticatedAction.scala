/*
 * Copyright 2017 HM Revenue & Customs
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
import config.Wiring
import connectors._
import models.{DetailedIndividualAccount, GroupAccount}
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.Play.current
import play.api.i18n.Messages.Implicits.applicationMessages

import scala.concurrent.Future

class AuthenticatedAction {
  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  val businessRatesAuthentication = Wiring().businessRatesAuthentication
  val groupAccounts = Wiring().groupAccountConnector
  val individualAccounts = Wiring().individualAccountConnector

  def apply(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthentication.authenticate flatMap { res => handleResult(res, body) }
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
      businessRatesAuthentication.authorise(authorisationId, assessmentRef) flatMap { res => handleResult(res, body) }
    }
  }

  def toViewAssessmentsFor(authorisationId: Long)(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthentication.authorise(authorisationId) flatMap { res => handleResult(res, body) }
  }

  private def handleResult(result: AuthorisationResult, body: BasicAuthenticatedRequest[AnyContent] => Future[Result])
                          (implicit request: Request[AnyContent]) = {
    result match {
      case Authenticated(accounts) => body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
      case InvalidGGSession => GovernmentGatewayProvider.redirectToLogin
      case NoVOARecord => Future.successful(Redirect(controllers.routes.CreateIndividualAccount.show))
      case IncorrectTrustId => Future.successful(Unauthorized("Trust ID does not match"))
      case NonOrganisationAccount => Future.successful(Redirect(controllers.routes.Application.invalidAccountType))
      case ForbiddenResponse => Future.successful(Forbidden(views.html.errors.forbidden()))
    }
  }
}

sealed trait AuthenticatedRequest[A] extends Request[A] {
  val organisationAccount: GroupAccount
  val individualAccount: DetailedIndividualAccount

  def organisationId: Int = organisationAccount.id
  def personId: Int = individualAccount.individualId
}

case class BasicAuthenticatedRequest[A](organisationAccount: GroupAccount, individualAccount: DetailedIndividualAccount, request: Request[A])
  extends WrappedRequest[A](request) with AuthenticatedRequest[A]

case class AgentRequest[A](organisationAccount: GroupAccount, individualAccount: DetailedIndividualAccount, agentCode: Long, request: Request[A])
  extends WrappedRequest[A](request) with AuthenticatedRequest[A]