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

import auth.GGAction
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import models.{IVDetails, IndividualDetails, PersonalDetails}
import play.api.libs.json.Reads
import session.{LinkingSessionRepository, WithAuthentication, WithLinkingSession}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

object Wiring {
  def apply() = play.api.Play.current.global.asInstanceOf[VPLFrontendGlobal].wiring
}

abstract class Wiring {
  val http: HttpGet with HttpPut with HttpDelete with HttpPost

  implicit lazy val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  lazy val sessionCache = new VPLSessionCache(http)
  lazy val sessionRepository = new LinkingSessionRepository(sessionCache)
  lazy val propertyConnector = new PropertyConnector(http)
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
  lazy val withAuthentication = new WithAuthentication
  lazy val addresses = new Addresses(http)
}

class VPLSessionCache(val http: HttpGet with HttpPut with HttpDelete)(implicit ec: ExecutionContext) extends SessionCache with AppName with ServicesConfig {
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

  private def getEntry[T](formId: String)(implicit hc: HeaderCarrier, rds: Reads[T]) = {
    fetchAndGetEntry(formId) map { _.getOrElse(throw new Exception(s"No keystore record found for $formId")) }
  }
}

class WSHttp extends WSGet with WSPut with WSDelete with WSPost with HttpAuditing with AppName with RunMode {
  override val hooks = Seq(AuditingHook)
  override def auditConnector = AuditServiceConnector
}

object ImplicitLifting {
  implicit def toFut[A](a: A): Future[A] = Future.successful(a)
  implicit def toOpt[A](a: A): Option[A] = Some(a)
}
