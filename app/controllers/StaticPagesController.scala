/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.i18n.Messages.implicitMessagesProviderToMessages
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.createAccount.termsAndConditions
import com.google.inject.Inject

class StaticPagesController @Inject()(
      override val controllerComponents: MessagesControllerComponents,
      termsAndConditionsView: termsAndConditions)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig
) extends FrontendBaseController {

  def termsAndConditions(): Action[AnyContent] = Action { implicit request =>
    Ok(termsAndConditionsView())
  }
}
