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

import auth.VoaAction
import config.ApplicationConfig
import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.config.ServicesConfig

class Register @Inject()(
                          ggAction: VoaAction
                        )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig, servicesConfig: ServicesConfig)
  extends PropertyLinkingController {

  def continue(accountType: String): Map[String, Seq[String]] = {
    Map("accountType" -> Seq(accountType), "continue" -> Seq(routes.Dashboard.home().url), "origin" -> Seq("voa"))
  }

  val show = Action { implicit request =>
    redirect("organisation")
  }

  val choice: Action[AnyContent] = Action { implicit request =>
    RegisterHelper.choiceForm.bindFromRequest().fold(
      errors => BadRequest(views.html.start(errors)),
      success =>
        redirect(success)
    )
  }

  def redirect(account: String): Result = {
    Redirect(
      config.ggRegistrationUrl,
      continue(account)
    )
  }
}

object RegisterHelper {
  val choiceForm = Form(single("choice" -> nonEmptyText))
}