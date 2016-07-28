/*
 * Copyright 2016 HM Revenue & Customs
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

import config.Wiring
import connectors.propertyLinking.ServiceContract.LinkedProperties
import play.api.mvc.Action
import session.WithAuthentication

object Dashboard extends PropertyLinkingController {
  val connector = Wiring().propertyLinkConnector

  def home() = Action { implicit request =>
    Ok(views.html.dashboard.home())
  }

  def manageProperties() = WithAuthentication.async { implicit request =>
    connector.linkedProperties(request.accountId).map { ps =>
      Ok(views.html.dashboard.manageProperties(ManagePropertiesVM(ps)))
    }
  }

  case class ManagePropertiesVM(properties: LinkedProperties)
}

