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

import config.ApplicationConfig.betaLoginPassword
import play.api.mvc.Action

trait BetaLoginController extends PropertyLinkingController {

  def show = Action { implicit request =>
    Ok(views.html.betaLogin())
  }

  def login = Action { implicit request =>
    (for {
      f <- request.body.asFormUrlEncoded
      ps <- f.get("betaloginkey")
      p <- ps.headOption if p == betaLoginPassword
    } yield {
      Redirect(routes.Dashboard.home).addingToSession("betaauthenticated" -> betaLoginPassword)
    }).getOrElse(BadRequest(views.html.betaLogin()))
  }
}

object BetaLoginController extends BetaLoginController
