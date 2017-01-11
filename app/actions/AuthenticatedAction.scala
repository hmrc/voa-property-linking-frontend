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

import scala.concurrent.Future

class AuthenticatedAction {
  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  val businessRatesAuthentication = Wiring().businessRatesAuthentication
  val groupAccounts = Wiring().groupAccountConnector
  val individualAccounts = Wiring().individualAccountConnector

  def apply(body: AuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthentication.authenticate flatMap {
      case Authenticated(ids) => body(AuthenticatedRequest(ids.organisationId, ids.personId, request))
      case InvalidGGSession => GovernmentGatewayProvider.redirectToLogin
      case NoVOARecord => Future.successful(Redirect(controllers.routes.CreateIndividualAccount.show))
      case IncorrectTrustId => Future.successful(Unauthorized("Trust ID does not match"))
    }
  }

  def withAccounts(body: DetailedAuthenticatedRequest[AnyContent] => Future[Result])(implicit messages: Messages) = apply { implicit request =>
    for {
      group <- groupAccounts.get(request.organisationId)
      individual <- individualAccounts.get(request.personId)
      res <- (group, individual) match {
        case (Some(g), Some(i)) => body(DetailedAuthenticatedRequest(g, i, request))
        case _ => Future.successful(BadRequest(views.html.errors.error()))
      }
    } yield {
      res
    }
  }

  def asAgent(body: AgentRequest[AnyContent] => Future[Result])(implicit messages: Messages) = withAccounts { implicit request =>
    request.organisationAccount.agentCode match {
      case Some(code) => body(AgentRequest(request.organisationAccount.id, request.individualAccount.individualId, code, request))
      case None => Future.successful(Unauthorized("Agent account required"))
    }
  }
}

case class AuthenticatedRequest[A](organisationId: Int, personId: Int, request: Request[A]) extends WrappedRequest[A](request)

case class DetailedAuthenticatedRequest[A](organisationAccount: GroupAccount, individualAccount: DetailedIndividualAccount, request: Request[A])
  extends WrappedRequest(request)

case class AgentRequest[A](organisationId: Int, personId: Int, agentCode: String, request: Request[A]) extends WrappedRequest[A](request)