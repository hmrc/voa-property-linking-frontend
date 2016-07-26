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
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import serialization.JsonFormats.accountFormat

object Registration extends PropertyLinkingController {
  val cache = Wiring().sessionCache

  def show() = Action { implicit request =>
    Ok(views.html.register(RegisterVM(registerForm)))
  }

  def submit() = Action.async { implicit request =>
    registerForm.bindFromRequest().fold(
      errors => BadRequest(views.html.register(RegisterVM(errors))),
      // TODO - the accountId will be eventually be supplied by the login mechanism and not dumped in the session
      formData => registerAccount(formData) map { _ =>
        Redirect(routes.Dashboard.home()).addingToSession("accountId" -> formData.companyName)
      }
    )
  }

  def registerAccount(account: Account)(implicit hc: HeaderCarrier) = {
    val a = cache.fetchAndGetEntry[Seq[Account]](accountsFormId) map {
      case Some(accounts) => accounts ++ Seq(account)
      case None => Seq(account)
    }
    a flatMap { cache.cache(accountsFormId, _) }
  }

  lazy val registerForm = Form(mapping(
    "companyName" -> nonEmptyText
  )(Account.apply)(Account.unapply))

}

case class RegisterVM(form: Form[_])

case class Account(companyName: String)
