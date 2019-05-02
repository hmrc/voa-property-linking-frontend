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
import connectors._
import javax.inject.Inject
import models.{DetailedIndividualAccount, GroupAccount}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

class AuthenticatedAction @Inject()(provider: GovernmentGatewayProvider,
                                    businessRatesAuthorisation: BusinessRatesAuthorisation,
                                    auth: Auth,
                                    addressesConnector: Addresses,
                                    val authConnector: AuthConnector)(implicit val messageApi: MessagesApi, config: ApplicationConfig) extends I18nSupport{

  override def messagesApi: MessagesApi = messageApi

  protected implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def apply(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    businessRatesAuthorisation.authenticate flatMap {
      res => handleResult(res, body)
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
      }
    }
  }

  def toViewAssessmentsFor(authorisationId: Long)(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthorisation.authorise(authorisationId) flatMap {
      res => handleResult(res, body)
    }
  }

  private def handleResult(result: AuthorisationResult, body: BasicAuthenticatedRequest[AnyContent] => Future[Result])
                          (implicit request: Request[AnyContent]) = {
    result match {
      case Authenticated(accounts)  => auth.success(accounts, body)
      case InvalidGGSession         => provider.redirectToLogin
      case NoVOARecord              => auth.noVoaRecord
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
