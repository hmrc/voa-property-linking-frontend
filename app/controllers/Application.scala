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

import auth.{GGAction, GGActionEnrolment, VoaAction}
import config.ApplicationConfig
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

class Application @Inject()(ggAction: VoaAction)(implicit val messagesApi: MessagesApi, config: ApplicationConfig) extends PropertyLinkingController {

  implicit override def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def addUserToGG = Action { implicit request =>
    Ok(views.html.addUserToGG())
  }

  def manageBusinessTaxAccount = Action { implicit request =>
    Redirect(config.businessTaxAccountUrl("manage-account"))
  }

  def start() = Action.async { implicit request =>
    ggAction match {
      case _: GGActionEnrolment =>
        Future.successful(Ok(views.html.start_enrolment(RegisterHelper.choiceForm)))
      case _: GGAction =>
        Future.successful(Ok(views.html.start()).withSession(SessionKeys.sessionId -> java.util.UUID.randomUUID().toString))
    }
  }

  def logOut() = Action { request =>
    Redirect(routes.Application.start()).withNewSession
  }

  def contactUs() = Action { implicit request =>
    Ok(views.html.contactUs())
  }

  def invalidAccountType = Action { implicit request =>
    Unauthorized(views.html.errors.invalidAccountType())
  }
}
