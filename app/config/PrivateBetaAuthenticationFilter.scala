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

import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport

import scala.concurrent.Future

object PrivateBetaAuthenticationFilter extends Filter with RunMode with MicroserviceFilterSupport {
  val unrestrictedPaths = Seq("betalogin", "ping/ping", "/business-rates-property-linking", "/login", "/register", "/registered", "/logout", "/start")

  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val isUnrestrictedPath = rh.path.contains("assets") || unrestrictedPaths.exists(rh.path.endsWith(_))
    (rh.session.get("betaauthenticated"), isUnrestrictedPath, ApplicationConfig.betaLoginRequired) match {
      case (None, false, true) =>
        Future.successful(Redirect(controllers.routes.BetaLoginController.show))
      case _ =>
        nextFilter(rh)
    }
  }
}
