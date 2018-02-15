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

import javax.inject.Inject

import config.ApplicationConfig
import play.api.mvc.{Action, Controller}

class Login @Inject()(config: ApplicationConfig) extends Controller {

  val continue = if (config.enrolmentEnabled){
    Map("continue" -> Seq(config.ggContinueUrl), "origin" -> Seq("voa"))
  } else {
    Map("origin" -> Seq("voa"), "accountType" -> Seq("organisation"), "continue" -> Seq(config.ggContinueUrl))
  }

  def show = Action { implicit request =>
    Redirect(config.ggSignInUrl, continue)
  }
}
