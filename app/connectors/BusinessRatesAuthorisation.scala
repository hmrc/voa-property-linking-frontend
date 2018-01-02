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

import config.{AuthorisationFailed, WSHttp}
import models.Accounts
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
class BusinessRatesAuthorisation @Inject()(config: ServicesConfig, http: WSHttp) {
  val url = config.baseUrl("business-rates-authorisation") + "/business-rates-authorisation"

  def authenticate(implicit hc: HeaderCarrier): Future[AuthorisationResult] = {
    http.GET[Accounts](s"$url/authenticate") map {
      Authenticated
    } recover {
      case AuthorisationFailed(err) => handleUnauthenticated(err)
    }
  }

  def authorise(authorisationId: Long, assessmentRef: Long)(implicit hc: HeaderCarrier): Future[AuthorisationResult] = {
    http.GET[Accounts](s"$url/property-link/$authorisationId/assessment/$assessmentRef") map {
      Authenticated
    } recover {
      case AuthorisationFailed(err) => handleUnauthenticated(err)
    }
  }

  def authorise(authorisationId: Long)(implicit hc: HeaderCarrier): Future[AuthorisationResult] = {
    http.GET[Accounts](s"$url/property-link/$authorisationId") map {
      Authenticated
    } recover {
      case AuthorisationFailed(err) => handleUnauthenticated(err)
      case Upstream4xxResponse(_, 403, _, _) => ForbiddenResponse
    }
  }

  private def handleUnauthenticated(error: String) = error match {
    case "INVALID_GATEWAY_SESSION" => InvalidGGSession
    case "NO_CUSTOMER_RECORD" => NoVOARecord
    case "TRUST_ID_MISMATCH" => IncorrectTrustId
    case "NON_ORGANISATION_ACCOUNT" => NonOrganisationAccount
  }

}

sealed trait AuthorisationResult

case class Authenticated(accounts: Accounts) extends AuthorisationResult

case object InvalidGGSession extends AuthorisationResult

case object NoVOARecord extends AuthorisationResult

case object IncorrectTrustId extends AuthorisationResult

case object NonOrganisationAccount extends AuthorisationResult

case object ForbiddenResponse extends AuthorisationResult