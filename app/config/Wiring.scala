/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.Inject
import play.api.libs.json.{JsDefined, JsString, Writes}
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.{HttpDelete, _}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws._

import scala.concurrent.Future
import scala.util.Try

class VPLHttp @Inject()(
                         environment: Environment,
                         servicesConfig: ServicesConfig,
                         override val appNameConfiguration: Configuration,
                         val auditConnector: AuditConnector) extends WSHttp with HttpAuditing {

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

  override protected def configuration: Option[Config] = Some(appNameConfiguration.underlying)
}

case class AuthorisationFailed(msg: String) extends Exception(s"Authorisation failed: $msg")

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with HttpPatch with WSPatch with Hooks with AppName {
  override protected def actorSystem: ActorSystem = Play.current.actorSystem
}

object WSHttp extends WSHttp {
  override protected def configuration: Option[Config] = None

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override def auditConnector: AuditConnector = Play.current.injector.instanceOf[AuditConnector]
}



trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
}

