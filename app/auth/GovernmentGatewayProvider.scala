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

import config.ApplicationConfig
import connectors.VPLAuthConnector
import models.enrolment.{UserDetails, UserInfo}
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext, GovernmentGateway}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GGAction @Inject()(val provider: GovernmentGatewayProvider, val authConnector: AuthConnector) extends Actions with VoaAction {
  type x = AuthContext

  private def authenticatedBy = AuthenticatedBy(provider, GGConfidence)

  def apply(body: AuthContext => Request[AnyContent] => Result): Action[AnyContent] = authenticatedBy(body)

  def async(isSession: Boolean)(body: AuthContext => Request[AnyContent] => Future[Result]): Action[AnyContent] = authenticatedBy.async(body)
}

class GGActionEnrolment @Inject()(val provider: GovernmentGatewayProvider, val authConnector: AuthConnector, vPLAuthConnector: VPLAuthConnector) extends VoaAction {

  import SessionHelpers._
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
        case _: Throwable => provider.redirectToLogin
      }

  private def userDetailsFromSession(body: UserDetails => Request[AnyContent] => Future[Result])
                                    (implicit request: Request[AnyContent]) = request.session.getUserDetails match {
      case Some(userDetails) => body(userDetails)(request)
      case None => async(isSession = true)(body)(request)
    }

}

object SessionHelpers {

  implicit class SessionOps(session: Session) {

    val keys = List("firstName", "lastName", "email", "postcode", "affinityGroup")

    def getUserDetails: Option[UserDetails] = {
      (session.get("externalId"), session.get("firstName"), session.get("lastName"), session.get("email"), session.get("postcode"), session.get("groupIdentifier"), session.get("affinityGroup")) match {
        case (Some(externalId), firstName, lastName, Some(email), postcode, Some(groupId), Some(affinityGroup)) =>
          Json.parse(affinityGroup)
            .asOpt[AffinityGroup]
            .flatMap(aff => Some(UserDetails(externalId, UserInfo(firstName, lastName, email, postcode, groupId, aff))))
        case _ =>
          None
      }
    }

    def putUserDetails(userDetails: UserDetails) = {
      session
        .+("firstName" -> userDetails.userInfo.firstName.getOrElse(""))
        .+("lastName" -> userDetails.userInfo.lastName.getOrElse(""))
        .+("email" -> userDetails.userInfo.email)
        .+("postcode" -> userDetails.userInfo.postcode.getOrElse(""))
        .+("affinityGroup" -> userDetails.userInfo.affinityGroup.toJson.toString)
    }

    def removeUserDetails = {
      session
        .-("firstName")
        .-("lastName")
        .-("email")
        .-("postcode")
        .-("affinityGroup")
    }
  }

}

trait VoaAction {
  type x

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def async(isSession: Boolean)(body: x => Request[AnyContent] => Future[Result]): Action[AnyContent]
}

class GovernmentGatewayProvider @Inject()(config: ApplicationConfig) extends GovernmentGateway {
  this: ServicesConfig =>
  override def additionalLoginParameters: Map[String, Seq[String]] = Map("accountType" -> Seq("organisation"))

  override def loginURL: String = config.ggSignInUrl

  override def continueURL = config.ggContinueUrl

  override def redirectToLogin(implicit request: Request[_]) = {
    Future.successful(Redirect(loginURL, Map("continue" -> Seq(config.baseUrl + request.uri), "origin" -> Seq("voa")) ++ additionalLoginParameters))
  }
}
