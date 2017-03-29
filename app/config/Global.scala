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

import akka.stream.Materializer
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Play.{configuration, current}
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Application, Configuration, GlobalSettings, Play}
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, ServicesConfig}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.frontend.bootstrap.{DefaultFrontendGlobal, ShowErrorPage}
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter
import play.api.http.DefaultHttpFilters

import scala.concurrent.Future

object Global extends VPLFrontendGlobal {
  override val wiring: Wiring = new Wiring {
    override def http = new WSHttp
  }
}

trait VPLFrontendGlobal extends DefaultFrontendGlobal with ShowErrorPage {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    views.html.errors.error(pageTitle, heading, message)(request, applicationMessages)
  }

  def auditConnector: uk.gov.hmrc.play.audit.http.connector.AuditConnector = AuditServiceConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("metrics")

  override def loggingFilter: FrontendLoggingFilter = LoggingFilter

  override def frontendAuditFilter: FrontendAuditFilter = AuditFilter

  val wiring: Wiring
}

object AuditServiceConnector extends AuditConnector {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object AuditFilter extends FrontendAuditFilter with MicroserviceFilterSupport with AppName {
  override lazy val maskedFormFields = Seq.empty
  override lazy val applicationPort = None
  override lazy val auditConnector = AuditServiceConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean = false
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

class Filters @Inject()(filter: WhitelistFilter) extends DefaultHttpFilters(filter)

class WhitelistFilter @Inject()(conf: Configuration)(implicit val mat: Materializer) extends AkamaiWhitelistFilter {
  lazy val enabled = conf.getBoolean("feature.whitelist").getOrElse(false)

  override def whitelist: Seq[String] = conf.getString("whitelist.ips")
    .map(str => str.split(",").toSeq).getOrElse(Seq())

  override def destination: Call = conf.getString("whitelist.destination")
    .map(url => Call("GET", url)).getOrElse(Call("GET", "https://www.gov.uk"))

  override def excludedPaths: Seq[Call] = conf.getString("whitelist.exclusions")
    .map[Seq[Call]](str => str.split(",").map(entry => Call("GET", entry))).getOrElse(Seq())

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (enabled) {
      super.apply(f)(rh)
    } else {
      f(rh)
    }
  }
}

