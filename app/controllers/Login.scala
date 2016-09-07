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
  val repo = Wiring().accountConnector

  def show() = Action.async { implicit request =>
    repo.get().map ( accounts =>
      Ok(views.html.login(LoginVM(loginForm, accounts)))
    )
  }

  def submit() = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      errors => {
        repo.get().map ( accounts =>
          BadRequest(views.html.login(LoginVM(errors, accounts)))
        )
      },
      account => {
        repo.get().map ( accounts =>
          accounts.find(_.companyName == account.companyName) match {
            case Some(acc: Account) =>{
              if (acc.isAgent)
                Redirect(controllers.agent.routes.Dashboard.home()).addingToSession("accountId" -> account.companyName)
              else
                Redirect(routes.Dashboard.home()).addingToSession("accountId" -> account.companyName)
            }
            case None =>
              BadRequest(views.html.login(LoginVM(loginForm.withError("companyName", "error.login.invalid"), accounts)))
          })
      }
    )
  }

  lazy val loginForm = Form(
    mapping(
      "companyName" -> nonEmptyText
    )(miniAccount.apply)(miniAccount.unapply)
  )
}
case class miniAccount(companyName: String)

case class LoginVM(form: Form[_], accounts: Seq[Account])
