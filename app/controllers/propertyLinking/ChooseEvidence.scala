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

package controllers.propertyLinking

import javax.inject.Inject
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import session.WithLinkingSession

import scala.concurrent.Future

class ChooseEvidence @Inject() (
                                 val withLinkingSession: WithLinkingSession
                               )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def show: Action[AnyContent] = withLinkingSession { implicit request =>
    Future.successful(Ok(views.html.propertyLinking.chooseEvidence(ChooseEvidence.form)))
  }

  def submit: Action[AnyContent] = withLinkingSession { implicit request =>
    ChooseEvidence.form.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.propertyLinking.chooseEvidence(errors))),
      {
        case true   => Future.successful(Redirect(routes.UploadRatesBill.show()))
        case false  => Future.successful(Redirect(routes.UploadEvidence.show()))
      }
    )
  }
}

object ChooseEvidence {
  lazy val form = Form(single(keys.hasRatesBill -> mandatoryBoolean))
  lazy val keys = new {
    val hasRatesBill = "hasRatesBill"
  }
}
