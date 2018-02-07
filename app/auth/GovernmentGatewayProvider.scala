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

import config.{ApplicationConfig, Global}
import connectors.VPLAuthConnector
import models.enrolment.{UserDetails, UserInfo}
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
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
        case e: BadRequestException =>
          Global.onBadRequest(request, e.message)
        case _: NotFoundException =>
          Global.onHandlerNotFound(request)
        //need to catch unhandled exceptions here to propagate the request ID into the internal server error page
        case e =>
          Global.onError(request, e)
      }

  private def userDetailsFromSession(body: UserDetails => Request[AnyContent] => Future[Result])
                                    (implicit request: Request[AnyContent]) = request.session.getUserDetails match {
      case Some(userDetails) =>
        body(userDetails)(request)
      case None =>
        async(isSession = true)(body)(request)
    }
}

object SessionHelpers {

  val key = new {
    val externalId = "externalId"
    val credId = "credId"
    val firstName = "firstName"
    val lastName = "lastName"
    val email = "email"
    val postcode = "postcode"
    val groupId = "groupId"
    val affinityGroup = "affinityGroup"
  }

  implicit class SessionOps(session: Session) {

    def getUserDetails: Option[UserDetails] = {
      (session.get(key.externalId), session.get(key.externalId), session.get(key.firstName), session.get(key.lastName), session.get(key.email), session.get(key.postcode), session.get(key.groupId), session.get(key.affinityGroup)) match {
        case (Some(externalId), Some(credId), firstName, lastName, Some(email), postcode, Some(groupId), Some(affinityGroup)) =>
          Json.parse(affinityGroup)
            .asOpt[AffinityGroup]
            .flatMap(aff => Some(UserDetails(externalId, credId, UserInfo(firstName, lastName, email, postcode, groupId, credId, aff))))
        case _ =>
          None
      }
    }

    def putUserDetails(userDetails: UserDetails) = {
      session
        .+(key.externalId -> userDetails.externalId)
        .+(key.credId -> userDetails.credId)
        .+(key.firstName -> userDetails.userInfo.firstName.getOrElse(""))
        .+(key.lastName -> userDetails.userInfo.lastName.getOrElse(""))
        .+(key.email -> userDetails.userInfo.email)
        .+(key.postcode -> userDetails.userInfo.postcode.getOrElse(""))
        .+(key.groupId -> userDetails.userInfo.groupIdentifier)
        .+(key.affinityGroup -> userDetails.userInfo.affinityGroup.toJson.toString)
    }

    def removeUserDetails = {
      session
        .-(key.externalId)
        .-(key.credId)
        .-(key.firstName)
        .-(key.lastName)
        .-(key.email)
        .-(key.postcode)
        .-(key.groupId)
        .-(key.affinityGroup)
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
