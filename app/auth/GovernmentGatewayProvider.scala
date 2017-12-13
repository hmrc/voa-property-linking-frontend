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
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext, GovernmentGateway}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.{AffinityGroup, InvalidBearerToken, NoActiveSession}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GGAction @Inject()(val provider: GovernmentGatewayProvider, val authConnector: AuthConnector) extends Actions with UnAuthAction {
  type x = AuthContext
  private def authenticatedBy = AuthenticatedBy(provider, GGConfidence)

  def apply(body: AuthContext => Request[AnyContent] => Result): Action[AnyContent] = authenticatedBy(body)
  def async(isSession: Boolean)(body: AuthContext => Request[AnyContent] => Future[Result]): Action[AnyContent] = authenticatedBy.async(body)
}

class GGActionEnrolment @Inject()(val provider: GovernmentGatewayProvider, val authConnector: AuthConnector, vPLAuthConnector: VPLAuthConnector) extends UnAuthAction {

  import SessionHelpers._

  type x = UserDetails
//  def apply(body: UserDetails => Request[AnyContent] => Result) = ???
  def async(isSession: Boolean)(body: UserDetails => Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    if(isSession) {
      vPLAuthConnector
        .getUserDetails
        .flatMap(x => body(x)(request).map(_.withSession(request.session.putUserDetails(x))))
    } else {
      request.session.getUserDetails match {
        case Some(x) => body(x)(request)
        case None => async(isSession = true)(body)(request)
      }
    }
  }
}

object SessionHelpers {

  implicit class SessionOps(session: Session) {

    val keys = List("firstName", "lastName", "email", "postcode", "affinityGroup")

    def getUserDetails: Option[UserDetails] = {
      (session.get("externalId"), session.get("firstName"), session.get("lastName"), session.get("email"), session.get("postcode"),  session.get("groupIdentifier"), session.get("affinityGroup")) match {
        case (Some(externalId), Some(firstName), Some(lastName), Some(email), Some(postcode), Some(groupId), Some(affinityGroup)) =>
          Json.parse(affinityGroup)
            .asOpt[AffinityGroup]
            .flatMap(aff => Some(UserDetails(externalId, UserInfo(firstName, lastName, email, postcode, groupId, aff))))
        case _ =>
          None
      }
    }

    def putUserDetails(userDetails: UserDetails) = {
      session
        .+("firstName" -> userDetails.userInfo.firstName)
        .+("lastName" -> userDetails.userInfo.lastName)
        .+("email" -> userDetails.userInfo.email)
        .+("postcode" -> userDetails.userInfo.postcode)
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

trait UnAuthAction {
  type x

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def async(isSession: Boolean)(body: x => Request[AnyContent] => Future[Result]): Action[AnyContent]
}

case class UserDetails(externalId: String, userInfo: UserInfo)

case class UserInfo(firstName: String, lastName: String, email: String, postcode: String, groupIdentifier: String, affinityGroup: AffinityGroup)
object UserDetails {
  implicit val userInfo = Json.format[UserInfo]
  implicit val format = Json.format[UserDetails]
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
