/*
 * Copyright 2018 HM Revenue & Customs
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

import auth.GGAction
import javax.inject.Inject
import config.ApplicationConfig
import play.api.mvc.Action
import uk.gov.hmrc.play.config.ServicesConfig

class Register @Inject()(config: ApplicationConfig, ggAction: GGAction) extends PropertyLinkingController with ServicesConfig {

  def show = Action { implicit request =>
    Redirect(
      config.ggRegistrationUrl,
      Map("accountType" -> Seq("organisation"), "continue" -> Seq(routes.Register.confirm().url), "origin" -> Seq("voa"))
    )
  }

  def confirm = ggAction { _ => implicit request =>
    Ok(views.html.ggRegistration())
  }
}
