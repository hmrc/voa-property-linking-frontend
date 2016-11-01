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
import connectors.PropertyRepresentation
import play.api.data.Form
import play.api.data.Forms._
import session.WithAuthentication

object AppointAgentController extends PropertyLinkingController {
  val propertyRepresentationConnector = Wiring().propertyRepresentationConnector

  def add(uarn: Long) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn).map(reprs => {
      if (reprs.nonEmpty)
        Ok(views.html.propertyRepresentation.alreadyAppointedAgent(uarn))
      else
        Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, uarn)))
    })
  }

  def edit(uarn: Long) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn).map(reprs => {
      if (reprs.size > 1)
        Ok(views.html.propertyRepresentation.selectAgent(reprs))
      else {
        val form = appointAgentForm.fill(AppointAgent(reprs.head.agentId, reprs.head.canCheck, reprs.head.canChallenge))
        Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form, uarn, reprs.head.representationId)))
      }
    })
  }

  def select(uarn: Long) = WithAuthentication.async{implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn).map(reprs => {
      Ok(views.html.propertyRepresentation.selectAgent(reprs))
    })
  }

  def appoint(uarn: Long) = WithAuthentication.async{ implicit request =>
    Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, uarn)))
  }

  def appointSubmit(uarn: Long) = WithAuthentication.async{ implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, uarn))),
      agent => {
        val reprRequest = PropertyRepresentation(java.util.UUID.randomUUID().toString, agent.agentCode, request.account.id,
          uarn, agent.canCheck, agent.canChallenge, true)
        propertyRepresentationConnector.create(reprRequest).map(_ => Ok(views.html.propertyRepresentation.appointedAgent()))
      }
    )
  }

  def modify(uarn: Long, agentCode:String) = WithAuthentication.async{ implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn).map( propReps => {
      val form = propReps.find(_.agentId == agentCode)
        .map(repr => {
          val reprId = repr.representationId
          (AppointAgent(repr.agentId, repr.canCheck, repr.canChallenge), reprId)
        })
        .map(x => (appointAgentForm.fill(x._1), x._2))
        .getOrElse((appointAgentForm, "0"))
      Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form._1, uarn, form._2)))
    })
  }

  def modifySubmit(uarn: Long, reprId: String) = WithAuthentication.async { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(errors, uarn, reprId))),
      agent => {
        val reprRequest = PropertyRepresentation(reprId, agent.agentCode, request.account.id,
          uarn, agent.canCheck, agent.canChallenge, true)
        propertyRepresentationConnector.update(reprRequest).map(_ => Ok(views.html.propertyRepresentation.modifiedAgent()))
      }
    )
  }

  lazy val appointAgentForm = Form(mapping(
    "agentCode" -> nonEmptyText,
    "canCheck" -> boolean,
    "canChallenge" -> boolean
  )(AppointAgent.apply)(AppointAgent.unapply))

  case class AppointAgent(agentCode: String, canCheck: Boolean, canChallenge: Boolean)
  case class AppointAgentVM(form: Form[_], uarn: Long)
  case class ModifyAgentVM(form: Form[_], uarn: Long, representationId: String)

}
