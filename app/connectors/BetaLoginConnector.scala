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

package connectors

import config.AuthorisationFailed
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

class BetaLoginConnector(http: HttpPost)(implicit ec: ExecutionContext) extends ServicesConfig {
  private lazy val url = baseUrl("property-linking") + "/property-linking"

  def login(password: String)(implicit hc: HeaderCarrier): Future[BetaLoginResponse] = {
    http.POST[JsValue, HttpResponse](url + "/betalogin", Json.obj("password" -> password)) map {
      case HttpResponse(200, _, _, _) => LoggedIn
    } recover {
      case Upstream4xxResponse(_, 401, _, _) => IncorrectPassword
      case AuthorisationFailed("LOCKED_OUT") => LockedOut
    }
  }
}

sealed trait BetaLoginResponse

case object LoggedIn extends BetaLoginResponse
case object IncorrectPassword extends BetaLoginResponse
case object LockedOut extends BetaLoginResponse