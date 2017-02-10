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

  def apply(body: BasicAuthenticatedRequest[AnyContent] => Future[Result]) = Action.async { implicit request =>
    businessRatesAuthentication.authenticate flatMap {
      case Authenticated(ids) => body(BasicAuthenticatedRequest(ids.organisationId, ids.personId, request))
      case InvalidGGSession => GovernmentGatewayProvider.redirectToLogin
      case NoVOARecord => Future.successful(Redirect(controllers.routes.CreateIndividualAccount.show))
      case IncorrectTrustId => Future.successful(Unauthorized("Trust ID does not match"))
    }
  }

  def withAccounts(body: DetailedAuthenticatedRequest[AnyContent] => Future[Result])(implicit messages: Messages) = apply { implicit request =>
    val eventualMaybeGroupAccount = groupAccounts.get(request.organisationId)
    val eventualMaybeIndividualAccount = individualAccounts.get(request.personId)

    for {
      group <- eventualMaybeGroupAccount
      individual <- eventualMaybeIndividualAccount
      res <- (group, individual) match {
        case (Some(g), Some(i)) => body(DetailedAuthenticatedRequest(g, i, request))
        case _ => throw new Exception(s"user with organisationId ${request.organisationId} " +
          s"and personId ${request.personId} has authenticated but accounts could not be retrieved")
      }
    } yield {
      res
    }
  }

  def asAgent(body: AgentRequest[AnyContent] => Future[Result])(implicit messages: Messages) = withAccounts { implicit request =>
    request.organisationAccount.isAgent match {
      case true => body(AgentRequest(request.organisationAccount.id, request.individualAccount.individualId, request.organisationAccount.agentCode, request))
      case false => Future.successful(Unauthorized("Agent account required"))
    }
  }
}

sealed trait AuthenticatedRequest[A] extends Request[A] {
  val organisationId: Int
  val personId: Int
}

case class BasicAuthenticatedRequest[A](organisationId: Int, personId: Int, request: Request[A])
  extends WrappedRequest[A](request) with AuthenticatedRequest[A]

case class DetailedAuthenticatedRequest[A](organisationAccount: GroupAccount, individualAccount: DetailedIndividualAccount, request: Request[A])
  extends WrappedRequest(request) with AuthenticatedRequest[A] {
  override val organisationId = organisationAccount.id
  override val personId = individualAccount.individualId
}

case class AgentRequest[A](organisationId: Int, personId: Int, agentCode: String, request: Request[A])
  extends WrappedRequest[A](request) with AuthenticatedRequest[A]