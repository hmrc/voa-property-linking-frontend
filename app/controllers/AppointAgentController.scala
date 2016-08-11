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

import connectors.propertyLinking.ServiceContract
import connectors.propertyLinking.ServiceContract.PropertyRepresentation
import play.api.data.Form
import play.api.data.Forms._
import session.WithAuthentication

object AppointAgentController extends PropertyLinkingController {

  def add = WithAuthentication.async { implicit request =>
   val propReprs = Seq(PropertyRepresentation("id1", "Agent123", request.accountId, "d", true, true, true) )

    if (propReprs.nonEmpty)
      Ok(views.html.agent.alreadyAppointedAgent())
    else
      Ok(views.html.agent.appointAgent(AppointAgentVM(appointAgentForm)))
  }

  def edit = WithAuthentication.async { implicit request =>
    val propReprs = Seq(
      PropertyRepresentation("id1", "Agent123", request.accountId, "d", true, true, true),
      PropertyRepresentation("id2", "Agent234", request.accountId, "d", true, true, false)
    )

    if (propReprs.size > 1)
      Ok(views.html.agent.selectAgent(propReprs))
    else
      Ok(views.html.agent.appointAgent(AppointAgentVM(appointAgentForm)))
  }

  def select = WithAuthentication.async{implicit request =>
    val propReprs = Seq(
      PropertyRepresentation("id1", "Agent123", request.accountId, "d", true, true, true),
      PropertyRepresentation("id2", "Agent234", request.accountId, "d", true, true, false)
    )
    Ok(views.html.agent.selectAgent(propReprs))
  }

  def appoint = WithAuthentication.async{ implicit request =>
    Ok(views.html.agent.appointAgent(AppointAgentVM(appointAgentForm)))
  }

  def appointSubmit = WithAuthentication.async{ implicit request =>
    Ok(views.html.agent.appointedAgent())
  }
  lazy val appointAgentForm = Form(mapping(
    "canCheck" -> boolean,
    "canChallenge" -> boolean
  )(AppointAgent.apply)(AppointAgent.unapply))

  case class AppointAgent(canCheck: Boolean, canChallenge: Boolean)
  case class AppointAgentVM(form: Form[_])

}
