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

import javax.inject.Inject

import actions.AuthenticatedAction
import auth.GGAction
import connectors._
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import connectors.propertyLinking.PropertyLinkConnector
import models.PersonalDetails
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsDefined, JsString, Reads, Writes}
import repositories.SessionRepository
import session.{AgentAppointmentSessionRepository, WithLinkingSession}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{DB, DefaultDB}
import uk.gov.hmrc.mongo.MongoConnector

import scala.concurrent.Future
import scala.util.Try

object Wiring {
  def apply() = play.api.Play.current.global.asInstanceOf[VPLFrontendGlobal].wiring
}

abstract class Wiring {
  def http: HttpGet with HttpPut with HttpDelete with HttpPost with HttpPatch
  def propertyRepresentationConnector = new PropertyRepresentationConnector(http)
  def propertyLinkConnector = new PropertyLinkConnector(http)
  def individualAccountConnector = new IndividualAccounts(http)
  def groupAccountConnector = new GroupAccounts(http)
  def authConnector = new VPLAuthConnector(http)
  def ggAction = new GGAction(authConnector)
  def identityVerification = new IdentityVerification(http)
  def addresses = new Addresses(http)
  def businessRatesAuthentication = new BusinessRatesAuthorisation(http)
  def authenticated = new AuthenticatedAction
  def submissionIdConnector = new SubmissionIdConnector(http)
  def identityVerificationProxyConnector = new IdentityVerificationProxyConnector(http)
  def dvrCaseManagement = new DVRCaseManagementConnector(http)
  def businessRatesValuation = new BusinessRatesValuationConnector(http)
  def trafficThrottleConnector = new TrafficThrottleConnector(http)
}

  //FIXME: move to sessionRepo, ,and rename
//class PropertyLinkingSessionRepository @Inject()(db: DB) extends SessionRepository("propertyLinking", db)
class VPLSessionCache @Inject()(db: DB) extends SessionRepository("personDetails", db)
//(val http: HttpGet with HttpPut with HttpDelete) extends SessionCache with AppName with ServicesConfig {


class WSHttp extends WSGet with WSPut with WSDelete with WSPost with WSPatch with HttpAuditing with AppName with RunMode {
  override val hooks = Seq(AuditingHook)
  override def auditConnector = AuditServiceConnector

  override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = super.doGet(url) map { res =>
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
