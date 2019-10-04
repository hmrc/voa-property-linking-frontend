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

package auth

import connectors.VPLAuthConnector
import javax.inject.Inject
import models.registration.UserDetails
import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

sealed trait VoaAction {

  type x

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def async(isSession: Boolean)(body: x => Request[AnyContent] => Future[Result]): Action[AnyContent]
}

class GgAction @Inject()(val provider: GovernmentGatewayProvider, vPLAuthConnector: VPLAuthConnector)(implicit executionContext: ExecutionContext) extends VoaAction {

  import utils.SessionHelpers._

  private val logger = Logger(this.getClass.getName)

  type x = UserDetails

  def async(isSession: Boolean)(body: UserDetails => Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    if (isSession) userDetailsWithoutSession(body) else userDetailsFromSession(body)
  }

  private def userDetailsWithoutSession(body: UserDetails => Request[AnyContent] => Future[Result])
                                       (implicit request: Request[AnyContent]) =
    vPLAuthConnector
      .getUserDetails
      .flatMap(userDetails => body(userDetails)(request).map(_.withSession(request.session.putUserDetails(userDetails))))
      .recoverWith {
        case e: Throwable =>
          logger.debug(e.getMessage, e)
          provider.redirectToLogin
      }

  private def userDetailsFromSession(body: UserDetails => Request[AnyContent] => Future[Result])
                                    (implicit request: Request[AnyContent]) = request.session.getUserDetails match {
    case Some(userDetails) =>
      body(userDetails)(request)
    case None =>
      async(isSession = true)(body)(request)
  }
}