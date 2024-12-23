/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

class Application @Inject() (
      val errorHandler: CustomErrorHandler,
      addUserToGGView: views.html.addUserToGG,
      invalidAccountTypeView: views.html.errors.invalidAccountType,
      startView: views.html.registration.start,
      startViewOldJourney: views.html.startOldJourney
)(implicit
      override val controllerComponents: MessagesControllerComponents,
      config: ApplicationConfig
) extends PropertyLinkingController {

  private val fallbackUrl = config.dashboardUrl("home")

  def addUserToGG: Action[AnyContent] =
    Action { implicit request =>
      Ok(addUserToGGView())
    }

  def manageBusinessTaxAccount: Action[AnyContent] = Action(Redirect(config.businessTaxAccountUrl("manage-account")))

  def start: Action[AnyContent] =
    Action { implicit request =>
      if (config.newRegistrationJourneyEnabled)
        Ok(startView(RegisterHelper.choiceForm))
      else
        Ok(startViewOldJourney(RegisterHelper.choiceForm))
    }

  def logOut: Action[AnyContent] = Action(Redirect(routes.Application.start).withNewSession)

  def invalidAccountType: Action[AnyContent] =
    Action { implicit request =>
      Unauthorized(invalidAccountTypeView())
    }

  def displayWelsh: Action[AnyContent] =
    Action { implicit request =>
      Redirect(url = request.headers.get(REFERER).getOrElse(fallbackUrl)).withLang(Lang("cy"))
    }

  def setDefaultLanguage: Action[AnyContent] =
    Action { implicit request =>
      Redirect(url = request.headers.get(REFERER).getOrElse(fallbackUrl)).withoutLang
    }
}
