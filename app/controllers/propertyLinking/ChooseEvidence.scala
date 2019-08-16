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
import session.WithLinkingSession

class ChooseEvidence @Inject() (val withLinkingSession: WithLinkingSession)
                               (implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def show = withLinkingSession { implicit request =>
    Ok(views.html.propertyLinking.chooseEvidence(ChooseEvidence.form))
  }

  def submit = withLinkingSession { implicit request =>
    ChooseEvidence.form.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyLinking.chooseEvidence(errors)),
      {
        case true => Redirect(routes.UploadRatesBill.show())
        case false => Redirect(routes.UploadPropertyEvidence.show())
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
