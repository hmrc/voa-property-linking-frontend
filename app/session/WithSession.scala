/*
 * Copyright 2016 HM Revenue & Customs
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

package session

import config.Wiring
import models.{GroupAccount, IndividualAccount}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

case class LinkingSessionRequest[A](ses: LinkingSession, groupId: String, request: Request[A]) extends WrappedRequest[A](request) {
  def sessionId: String = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session)).sessionId.map(_.value).getOrElse(throw NoSessionId)
}

case object NoSessionId extends Exception

class WithLinkingSession {
  implicit def hc(implicit request: Request[_]) = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
  val session = Wiring().sessionRepository
  val accountRepo = Wiring().individualAccountConnector
  val userDetails = Wiring().userDetailsConnector
  val ggAction = Wiring().ggAction

  def apply(body: LinkingSessionRequest[AnyContent] => Future[Result]) = ggAction.async { ctx => implicit request =>
    for {
      groupId <- userDetails.getGroupId(ctx)
      sOpt <- session.get
      res <- sOpt match {
        case Some(s) => body(LinkingSessionRequest(s, groupId, request))
        case None => Future.successful(Unauthorized("No linking session"))
      }
    } yield {
      res
    }
  }
}

case class AuthenticatedRequest[A](account: GroupAccount, request: Request[A]) extends WrappedRequest[A](request)

object WithAuthentication {
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val ggAction = Wiring().ggAction
  val userDetails = Wiring().userDetailsConnector
  val auth = Wiring().authConnector

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  def apply(body: AuthenticatedRequest[AnyContent] => Future[Result]) = ggAction.async { ctx => implicit request =>
    for {
      userId <- auth.getInternalId(ctx)
      groupId <- userDetails.getGroupId(ctx)
      userAccount <- individuals.get(userId)
      groupAccount <- groups.get(groupId)
      res <- (userAccount, groupAccount) match {
        case (u, g) if u.isEmpty || g.isEmpty => Future.successful(Redirect(controllers.routes.CreateIndividualAccount.show))
        case (Some(u), Some(g)) => body(AuthenticatedRequest(g, request))
      }
    } yield {
      res
    }
  }

  def async(body: AuthenticatedRequest[AnyContent] => Future[Result]) = apply(body)
}