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

package controllers.agent

import config.Wiring
import connectors.PropertyRepresentation
import controllers.PropertyLinkingController
import session.WithAuthentication

object Representation extends PropertyLinkingController{
  val reprConnector = Wiring().propertyRepresentationConnector

  def manageRepresentationRequest() = WithAuthentication.async { implicit request =>
    reprConnector.forAgent(request.account.id).map{ reprs =>
      Ok(views.html.agent.dashboard.propertyRepresentation.manageProperties(ManagePropertiesVM(reprs, request.account.id)))
    }
  }

  def accept(reprId: String) = WithAuthentication.async { implicit request =>
    reprConnector.accept(reprId).map{ _ =>
      Redirect(routes.Representation.manageRepresentationRequest())
    }
  }

  def reject(reprId: String) = WithAuthentication.async { implicit request =>
    reprConnector.reject(reprId).map{ _ =>
      Redirect(routes.Representation.manageRepresentationRequest())
    }
  }

  case class ManagePropertiesVM(propertyRepresentations: Seq[PropertyRepresentation], agentCode: String)

}
