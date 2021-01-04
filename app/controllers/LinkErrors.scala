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
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

class LinkErrors @Inject()(
      val errorHandler: CustomErrorHandler,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def manualVerificationRequired() = Action { implicit request =>
    Ok(views.html.linkErrors.manualVerificationRequired())
  }

  def conflict() = Action { implicit request =>
    Ok(views.html.linkErrors.conflict())
  }

}
