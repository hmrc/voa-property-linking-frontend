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

package config

import actions.AuthenticatedAction
import auth.GGAction
import connectors._
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import connectors.propertyLinking.PropertyLinkConnector
import models.{IVDetails, IndividualDetails, PersonalDetails}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsDefined, JsString, Reads, Writes}
import session.{LinkingSessionRepository, WithLinkingSession}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

import scala.concurrent.Future
import scala.util.Try

object Wiring {
  def apply() = play.api.Play.current.global.asInstanceOf[VPLFrontendGlobal].wiring
}

abstract class Wiring {
  val http: HttpGet with HttpPut with HttpDelete with HttpPost

  lazy val sessionCache = new VPLSessionCache(http)
  lazy val sessionRepository = new LinkingSessionRepository(sessionCache)
  lazy val propertyRepresentationConnector = new PropertyRepresentationConnector(http)
  lazy val propertyLinkConnector = new PropertyLinkConnector(http)
  lazy val individualAccountConnector = new IndividualAccounts(http)
  lazy val groupAccountConnector = new GroupAccounts(http)
  lazy val ratesBillVerificationConnector = new RatesBillVerificationConnector(http)
  lazy val authConnector = new VPLAuthConnector(http)
  lazy val ggAction = new GGAction(authConnector)
  lazy val withLinkingSession = new WithLinkingSession
  lazy val fileSystemConnector = FileSystemConnector
  lazy val identityVerification = new IdentityVerification(http)
  lazy val addresses = new Addresses(http)
  lazy val businessRatesAuthentication = new BusinessRatesAuthorisation(http)
  lazy val authenticated = new AuthenticatedAction
  lazy val betaLoginConnector = new BetaLoginConnector(http)
  lazy val submissionIdConnector = new SubmissionIdConnector(http)
  lazy val identityVerificationProxyConnector = new IdentityVerificationProxyConnector(http)
  lazy val dvrCaseManagement = new DVRCaseManagementConnector(http)
  lazy val businessRatesValuation = new BusinessRatesValuationConnector(http)
}

class VPLSessionCache(val http: HttpGet with HttpPut with HttpDelete) extends SessionCache with AppName with ServicesConfig {
  override def defaultSource: String = appName
  override def baseUri: String = baseUrl("cachable.session-cache")
  override def domain: String = getConfString("cachable.session-cache.domain", throw new Exception("No config setting for cache domain"))

  def getIndividualDetails(implicit hc: HeaderCarrier) = getEntry[IndividualDetails]("individualDetails")
  def getIvDetails(implicit hc: HeaderCarrier) = getEntry[IVDetails]("ivDetails")

  def cachePersonalDetails(details: PersonalDetails)(implicit hc: HeaderCarrier) = {
    for {
      _ <- cache("ivDetails", details.ivDetails)
      _ <- cache("individualDetails", details.individualDetails)
    } yield {
      ()
    }
  }

  def cacheIndividualDetails(details: IndividualDetails)(implicit hc: HeaderCarrier) = cache("individualDetails", details) map { _ => () }

  private def getEntry[T](formId: String)(implicit hc: HeaderCarrier, rds: Reads[T]) = {
    fetchAndGetEntry(formId) map { _.getOrElse(throw new Exception(s"No keystore record found for $formId")) }
  }
}

class WSHttp extends WSGet with WSPut with WSDelete with WSPost with HttpAuditing with AppName with RunMode {
  override val hooks = Seq(AuditingHook)
  override def auditConnector = AuditServiceConnector

  override def doGet(url: String)(implicit hc: HeaderCarrier) = super.doGet(url) map { res =>
    res.status match {
      case 401 if hasJsonBody(res) => res.json \ "errorCode" match {
        case JsDefined(JsString(err)) => throw AuthorisationFailed(err)
        case _ => res
      }
      case _ => res
    }
  }


  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    super.doPost(url, body, headers) map { res =>
      res.status match {
        case 401 if hasJsonBody(res) => res.json \ "errorCode" match {
          case JsDefined(JsString(err)) => throw AuthorisationFailed(err)
          case _ => res
        }
        case _ => res
      }
    }
  }

  private def hasJsonBody(res: HttpResponse) = Try { res.json }.isSuccess
}

case class AuthorisationFailed(msg: String) extends Exception
