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

import config.Wiring
import connectors.{LockedOut, LoggedIn}
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.mvc.Action

trait BetaLoginController extends PropertyLinkingController {
  val loginConnector = Wiring().betaLoginConnector

  def show = Action { implicit request =>
    Ok(views.html.betaLogin())
  }

  def login = Action.async(parse.form(loginForm)) { implicit request =>
    loginConnector.login(request.body) map {
      case LoggedIn => Redirect(routes.Dashboard.home).addingToSession("betaauthenticated" -> "")
      case LockedOut => Unauthorized(views.html.lockedOut())
      case _ => BadRequest(views.html.betaLogin(showError = true))
    }
  }

  private lazy val loginForm = Form(Forms.single("betaloginkey" -> nonEmptyText))
}

object BetaLoginController extends BetaLoginController
