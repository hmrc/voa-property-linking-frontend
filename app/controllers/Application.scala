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

package controllers

import config.{ApplicationConfig, Wiring}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

object Application extends Controller with WithThrottling {
  val ggAction = Wiring().ggAction

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  def typography = Action { implicit request =>
    Ok(views.html.typography())
  }

  def releaseNotes = Action { implicit request =>
    if (ApplicationConfig.showReleaseNotes) {
      Ok(views.html.releaseNotes())
    } else {
      Redirect(routes.Application.start())
    }
  }

  def start() = Action.async { implicit request =>
    withThrottledHoldingPage("registration", Ok(views.html.errors.errorRegistration())) {
      Ok(views.html.start()).withSession(SessionKeys.sessionId -> java.util.UUID.randomUUID().toString)
    }
  }

  def logOut() = Action { request =>
    Redirect(routes.Application.start()).withNewSession
  }

  def contactUs() = Action { implicit request =>
    Ok(views.html.contactUs())
  }

  def invalidAccountType = Action { implicit request =>
    Unauthorized(views.html.errors.invalidAccountType())
  }

}
