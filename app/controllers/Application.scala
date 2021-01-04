/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

class Application @Inject()(
      val errorHandler: CustomErrorHandler
)(
      implicit override val controllerComponents: MessagesControllerComponents,
      config: ApplicationConfig
) extends PropertyLinkingController {

  def addUserToGG = Action { implicit request =>
    Ok(views.html.addUserToGG())
  }

  def manageBusinessTaxAccount = Action(Redirect(config.businessTaxAccountUrl("manage-account")))

  def start() = Action { implicit request =>
    Ok(views.html.start(RegisterHelper.choiceForm))
  }

  def logOut() = Action(Redirect(routes.Application.start()).withNewSession)

  def contactUs() = Action { implicit request =>
    Ok(views.html.contactUs())
  }

  def invalidAccountType = Action { implicit request =>
    Unauthorized(views.html.errors.invalidAccountType())
  }
}
