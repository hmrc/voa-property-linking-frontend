/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Action
import serialization.JsonFormats.accountFormat

object Login extends PropertyLinkingController {
  val cache = Wiring().sessionCache

  def show() = Action { implicit request =>
    Ok(views.html.login(LoginVM(loginForm, Wiring().tmpInMemoryAccountDb.toSeq)))
  }

  def submit() = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      errors => BadRequest(views.html.login(LoginVM(errors, Wiring().tmpInMemoryAccountDb.toSeq))),
      account => {
        if (Wiring().tmpInMemoryAccountDb.contains(account))
          Redirect(routes.Dashboard.home()).addingToSession("accountId" -> account.companyName)
        else
          BadRequest(views.html.login(LoginVM(loginForm.withError("companyName", "error.login.invalid"), Wiring().tmpInMemoryAccountDb.toSeq)))
      }
    )
  }

  lazy val loginForm = Form(mapping(
    "companyName" -> nonEmptyText
  )(Account.apply)(Account.unapply))
}

case class LoginVM(form: Form[_], accounts: Seq[Account])
