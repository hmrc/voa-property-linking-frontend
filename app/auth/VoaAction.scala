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

package auth

import javax.inject.Inject

import config.Global
import connectors.VPLAuthConnector
import models.enrolment.UserDetails
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait VoaAction {
  type x

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def async(isSession: Boolean)(body: x => Request[AnyContent] => Future[Result]): Action[AnyContent]
}

class GGAction @Inject()(val provider: GovernmentGatewayProvider, val authConnector: AuthConnector) extends Actions with VoaAction {
  type x = AuthContext

  private def authenticatedBy = AuthenticatedBy(provider, GGConfidence)

  def apply(body: AuthContext => Request[AnyContent] => Result): Action[AnyContent] = authenticatedBy(body)

  def async(isSession: Boolean)(body: AuthContext => Request[AnyContent] => Future[Result]): Action[AnyContent] = authenticatedBy.async(body)
}

class GGActionEnrolment @Inject()(val provider: GovernmentGatewayProvider, val authConnector: AuthConnector, vPLAuthConnector: VPLAuthConnector) extends VoaAction {

  import utils.SessionHelpers._
  type x = UserDetails

  def async(isSession: Boolean)(body: UserDetails => Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    if (isSession) userDetailsWithoutSession(body) else userDetailsFromSession(body)
  }

  private def userDetailsWithoutSession(body: UserDetails => Request[AnyContent] => Future[Result])
                                       (implicit request: Request[AnyContent], ec: ExecutionContext) =
    vPLAuthConnector
      .getUserDetails
      .flatMap(userDetails => body(userDetails)(request).map(_.withSession(request.session.putUserDetails(userDetails))))
      .recoverWith {
        case e: BadRequestException =>
          Global.onBadRequest(request, e.message)
        case _: NotFoundException =>
          Global.onHandlerNotFound(request)
        //need to catch unhandled exceptions here to propagate the request ID into the internal server error page
        case e =>
          Global.onError(request, e)
      }

  private def userDetailsFromSession(body: UserDetails => Request[AnyContent] => Future[Result])
                                    (implicit request: Request[AnyContent], ec: ExecutionContext) = request.session.getUserDetails match {
    case Some(userDetails) =>
      body(userDetails)(request)
    case None =>
      async(isSession = true)(body)(request)
  }
}