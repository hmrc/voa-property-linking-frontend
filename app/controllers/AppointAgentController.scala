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
import form.EnumMapping
import models.{AgentPermission, AgentPermissions}
import play.api.data.Form
import play.api.data.Forms._
import session.WithAuthentication

import scala.concurrent.Future

object AppointAgentController extends PropertyLinkingController {
  val propertyRepresentationConnector = Wiring().propertyRepresentationConnector
  val properties = Wiring().propertyConnector
  val accounts = Wiring().groupAccountConnector

  def add(uarn: Long) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn).map(reprs => {
      if (reprs.nonEmpty)
        Ok(views.html.propertyRepresentation.alreadyAppointedAgent(SelectAgentVM(reprs, uarn)))
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

  def select(uarn: Long) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn).map(reprs => {
      Ok(views.html.propertyRepresentation.selectAgent(reprs))
    })
  }

  def appoint(uarn: Long) = WithAuthentication.async { implicit request =>
    Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, uarn)))
  }

  def appointSubmit(uarn: Long) = WithAuthentication.async { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, uarn))),
      agent => {
        for {
          account <- accounts.get(agent.agentCode)
          prop <- properties.find(uarn)
          res <- (account, prop) match {
            case (Some(a), Some(p)) =>
              val req = PropertyRepresentation(java.util.UUID.randomUUID().toString, a.id, a.companyName, request.account.id,
                request.account.companyName, uarn, p.address, agent.canCheck, agent.canChallenge, true
              )
              propertyRepresentationConnector.create(req) map { _ => Ok(views.html.propertyRepresentation.appointedAgent(p.address, a.companyName)) }
            case _ => Future.successful(internalServerError)
          }
        } yield {
          res
        }
      }
    )
  }

  def modify(uarn: Long, agentCode: String) = WithAuthentication.async { implicit request =>
    propertyRepresentationConnector.get(request.account.id, uarn) map {
      _.find(_.agentId == agentCode) match {
        case Some(rep) =>
          val form = appointAgentForm.fill(AppointAgent(agentCode, rep.canCheck, rep.canChallenge))
          Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form, uarn, rep.representationId)))
        case None => internalServerError
      }
    }
  }

  def modifySubmit(uarn: Long, reprId: String) = WithAuthentication.async { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(errors, uarn, reprId))),
      agent => for {
        account <- accounts.get(agent.agentCode)
        prop <- properties.find(uarn)
        res <- (account, prop) match {
          case (Some(a), Some(p)) =>
            val req = PropertyRepresentation(java.util.UUID.randomUUID().toString, a.id, a.companyName, request.account.id,
              request.account.companyName, uarn, p.address, agent.canCheck, agent.canChallenge, true
            )
            propertyRepresentationConnector.create(req) map { _ => Ok(views.html.propertyRepresentation.modifiedAgent()) }
          case _ => Future.successful(internalServerError)
        }
      } yield {
        res
      }
    )
  }

  lazy val appointAgentForm = Form(mapping(
    "agentCode" -> nonEmptyText,
    "canCheck" -> EnumMapping(AgentPermissions),
    "canChallenge" -> EnumMapping(AgentPermissions)
  )(AppointAgent.apply)(AppointAgent.unapply))

  case class AppointAgent(agentCode: String, canCheck: AgentPermission, canChallenge: AgentPermission)

  case class AppointAgentVM(form: Form[_], uarn: Long)

  case class ModifyAgentVM(form: Form[_], uarn: Long, representationId: String)

  case class SelectAgentVM(reps: Seq[PropertyRepresentation], uarn: Long)

}
