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
import config.ImplicitLifting._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.{BadRequest, Forbidden}
import play.api.mvc.{ActionBuilder, ActionRefiner, Request, WrappedRequest}
import uk.gov.hmrc.play.http.HeaderCarrier

case class LinkingSessionRequest[A](ses: LinkingSession, accountId: String, request: Request[A]) extends WrappedRequest[A](request)

object WithLinkingSession extends ActionBuilder[LinkingSessionRequest] with ActionRefiner[Request, LinkingSessionRequest] {
  val repo = Wiring().sessionRepository

  implicit def hc(request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  override protected def refine[A](request: Request[A]) =
    repo.get()(hc(request)) map {
      case Some(x) => Right(LinkingSessionRequest(x, request.session.get("accountId").getOrElse(throw new Exception("No Account ID")), request))
      case None => Left(BadRequest(s"Invalid Session"))
    }

}

case class AuthenticatedRequest[A](accountId: String, request: Request[A]) extends WrappedRequest[A](request)

object WithAuthentication extends ActionBuilder[AuthenticatedRequest] with ActionRefiner[Request, AuthenticatedRequest] {

  override protected def refine[A](request: Request[A]) =
    request.session.get("accountId") match {
      case Some(aid) => Right(AuthenticatedRequest(aid, request))
      case None => Left(Forbidden)
    }
}