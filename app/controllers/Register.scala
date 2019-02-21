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

package controllers

import javax.inject.Inject
import auth.VoaAction
import config.ApplicationConfig
import play.api.Mode.Mode
import play.api.{Configuration, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.play.config.ServicesConfig

class Register @Inject()(ggAction: VoaAction)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig, servicesConfig: ServicesConfig) extends PropertyLinkingController {

  def continue(accountType: String) = {
    Map("accountType" -> Seq(accountType), "continue" -> Seq(routes.Dashboard.home().url), "origin" -> Seq("voa"))
  }

  def show() = Action { implicit request =>
    redirect("organisation")
  }

  def choice = Action.async { implicit request =>
    RegisterHelper.choiceForm.bindFromRequest().fold(
      errors => BadRequest(views.html.start(errors)),
      success =>
        redirect(success)
    )
  }

  def redirect(account: String) = {
    Redirect(
      config.ggRegistrationUrl,
      continue(account)
    )
  }
}

object RegisterHelper {
  val choiceForm = Form(single("choice" -> nonEmptyText))
}