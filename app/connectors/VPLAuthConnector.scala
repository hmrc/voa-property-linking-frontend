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

package connectors

import javax.inject.Inject

import auth.{UserDetails, UserInfo}
import config.WSHttp
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json, OFormat, Reads}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority

import scala.concurrent.Future

class VPLAuthConnector @Inject()(serverConfig: ServicesConfig, val http: WSHttp) extends AuthConnector {
  implicit val format = Json.format[CredId]
  implicit val userInfo: OFormat[UserInfo] = Json.format[UserInfo]
  implicit val userDetailsLink = Json.format[UserDetailsLink]
  implicit val externalIdFormat = Json.format[ExternalId]

  override val serviceUrl: String = serverConfig.baseUrl("auth")

  def getExternalId[A](ctx: A)(implicit hc: HeaderCarrier): Future[String] = ctx match {
    case x: AuthContext => getExternalId(x)
    case y: UserDetails => getExternalId(y)
  }

  private def getExternalId(ctx: AuthContext)(implicit hc: HeaderCarrier): Future[String] = getIds[JsValue](ctx) map { r =>
    (r \ "externalId").as[String]
  }

  private def getExternalId(userDetails: UserDetails): Future[String] =
    Future.successful(userDetails.externalId)

  def getGroupId[A](ctx: A)(implicit hc: HeaderCarrier): Future[String] = ctx match {
    case x: AuthContext => getGroupId(x)
    case y: UserDetails => getGroupId(y)
  }

  private def getGroupId(authContext: AuthContext)(implicit hc: HeaderCarrier): Future[String] = getUserDetails[JsValue](authContext) map { r =>
    (r \ "groupIdentifier").as[String]
  }

  private def getGroupId(userDetails: UserDetails): Future[String] =
    Future.successful(userDetails.userInfo.groupIdentifier)

  def getUserId(implicit hc: HeaderCarrier): Future[String] =
    getAuthority[CredId].map(_.credId)

  def getUserDetails(implicit hc: HeaderCarrier): Future[UserDetails] =
    getAuthority[UserDetailsLink]
      .flatMap{x =>
        for {
          id          <- http.GET[ExternalId](s"$serviceUrl${x.ids}")
          userInfo    <- http.GET[UserInfo](x.userDetailsLink)
        } yield UserDetails(id.externalId, userInfo)
      }

  private def getAuthority[A: Reads](implicit hc: HeaderCarrier) =
    http.GET[JsValue](s"$serviceUrl/auth/authority").map(_.as[A])

  def userDetails[A](ctx: A)(implicit hc: HeaderCarrier): Future[UserDetails] = ctx match {
      case x: AuthContext =>
        for {
          ids       <- this.getIds[ExternalId](x)
          userInfo  <- this.getUserDetails[JsValue](x)
          _         =  Logger.debug(Json.prettyPrint(userInfo) + " this is here")
          x         <- this.getUserDetails[UserInfo](x)
        } yield {
          UserDetails(ids.externalId, x)
        }
      case y: UserDetails => Future.successful(y)
    }

}

case class CredId(credId: String)

case class ExternalId(externalId: String)

case class UserDetailsLink(userDetailsLink: String, ids: String)
