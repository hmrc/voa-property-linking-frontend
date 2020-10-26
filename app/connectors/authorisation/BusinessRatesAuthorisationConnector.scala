/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors.authorisation

import config.AuthorisationFailed
import connectors.authorisation.errorhandler.exceptions.AuthorisationExceptionThrowingReads
import javax.inject.Inject
import models.{Accounts, PropertyLinkIds}
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesAuthorisationConnector @Inject()(
      config: ServicesConfig,
      http: HttpClient
)(implicit executionContext: ExecutionContext)
    extends AuthorisationExceptionThrowingReads {

  val url: String = config.baseUrl("business-rates-authorisation") + "/business-rates-authorisation"

  import AuthorisationResult._

  def authenticate(implicit hc: HeaderCarrier): Future[AuthorisationResult] =
    http
      .GET[Accounts](s"$url/authenticate")
      .map(Authenticated.apply)
      .recover {
        case AuthorisationFailed(err) => handleUnauthenticated(err)
      }

  private def handleUnauthenticated(error: String) = error match {
    case "INVALID_GATEWAY_SESSION" => InvalidGGSession
    case "NO_CUSTOMER_RECORD"      => NoVOARecord
    case "TRUST_ID_MISMATCH"       => IncorrectTrustId
    case "INVALID_ACCOUNT_TYPE"    => InvalidAccountType
    case "NON_GROUPID_ACCOUNT"     => NonGroupIDAccount
  }

  def isAgentOwnProperty(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Boolean] =
    http.GET[PropertyLinkIds](s"$url/$authorisationId/ids") map { ids =>
      ids.caseCreator.organisationId == ids.interestedParty.organisationId
    } recover { case UpstreamErrorResponse.WithStatusCode(403, _) => throw new ForbiddenException("Not Authorised") }

}

sealed trait AuthorisationResult

object AuthorisationResult {

  case class Authenticated(accounts: Accounts) extends AuthorisationResult

  case object InvalidGGSession extends AuthorisationResult

  case object NoVOARecord extends AuthorisationResult

  case object IncorrectTrustId extends AuthorisationResult

  case object InvalidAccountType extends AuthorisationResult

  case object ForbiddenResponse extends AuthorisationResult

  case object NonGroupIDAccount extends AuthorisationResult

}
