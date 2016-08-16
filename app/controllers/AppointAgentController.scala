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
import connectors.ServiceContract
import connectors.ServiceContract.PropertyRepresentation
import connectors.PropertyRepresentationConnector
import play.api.data.Form
import play.api.data.Forms._
import session.WithAuthentication
import uk.gov.hmrc.play.http.HeaderCarrier

object AppointAgentController extends PropertyLinkingController {
  val propertyRepresentationConnector = Wiring().propertyRepresentationConnector

  def add(uarn: String) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.companyName, uarn).map(reprs => {
      if (reprs.nonEmpty)
        Ok(views.html.agent.alreadyAppointedAgent(uarn))
      else
        Ok(views.html.agent.appointAgent(AppointAgentVM(appointAgentForm, uarn)))
    })
  }

  def edit(uarn: String) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.companyName, uarn).map(reprs => {
      if (reprs.size > 1)
        Ok(views.html.agent.selectAgent(reprs))
      else {
        val form = appointAgentForm.fill(AppointAgent(reprs(0).agentId, reprs(0).canCheck, reprs(0).canChallenge))
        Ok(views.html.agent.modifyAgent(ModifyAgentVM(form, uarn, reprs(0).representationId)))
      }
    })
  }

  def select(uarn: String) = WithAuthentication.async{implicit request =>
    propertyRepresentationConnector.get(request.account.companyName, uarn).map(reprs => {
      Ok(views.html.agent.selectAgent(reprs))
    })
  }

  def appoint(uarn: String) = WithAuthentication.async{ implicit request =>
    Ok(views.html.agent.appointAgent(AppointAgentVM(appointAgentForm, uarn)))
  }

  def appointSubmit(uarn: String) = WithAuthentication.async{ implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.agent.appointAgent(AppointAgentVM(errors, uarn))),
      agent => {
        val reprRequest = PropertyRepresentation(java.util.UUID.randomUUID().toString, agent.agentCode, request.account.companyName,
          uarn, agent.canCheck, agent.canChallenge, false)
        propertyRepresentationConnector.create(reprRequest).map(_ => Ok(views.html.agent.appointedAgent()))
      }
    )
  }

  def modify(uarn: String, agentCode:String) = WithAuthentication.async{ implicit request =>
    propertyRepresentationConnector.get(request.account.companyName, uarn).map( propReps => {
      val form = propReps.filter(_.agentId ==agentCode)
        .headOption
        .map(repr => {
          val reprId = repr.representationId
          (AppointAgent(repr.agentId, repr.canCheck, repr.canChallenge), reprId)
        })
        .map(x => (appointAgentForm.fill(x._1), x._2))
        .getOrElse((appointAgentForm, "0"))
      Ok(views.html.agent.modifyAgent(ModifyAgentVM(form._1, uarn, form._2)))
    })
  }

  def modifySubmit(uarn: String, reprId: String) = WithAuthentication.async { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.agent.modifyAgent(ModifyAgentVM(errors, uarn, reprId))),
      agent => {
        val reprRequest = PropertyRepresentation(reprId, agent.agentCode, request.account.companyName,
          uarn, agent.canCheck, agent.canChallenge, false)
        propertyRepresentationConnector.update(reprRequest).map(_ => Ok(views.html.agent.modifiedAgent()))
      }
    )
  }

  lazy val appointAgentForm = Form(mapping(
    "agentCode" -> nonEmptyText,
    "canCheck" -> boolean,
    "canChallenge" -> boolean
  )(AppointAgent.apply)(AppointAgent.unapply))

  case class AppointAgent(agentCode: String, canCheck: Boolean, canChallenge: Boolean)
  case class AppointAgentVM(form: Form[_], uarn: String)
  case class ModifyAgentVM(form: Form[_], uarn: String, representationId: String)

}
