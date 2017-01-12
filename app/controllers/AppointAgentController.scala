/*
 * Copyright 2017 HM Revenue & Customs
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
import connectors.{PropertyRepresentation, UpdatedRepresentation}
import form.EnumMapping
import models.{AgentPermission, AgentPermissions, NotPermitted}
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.mvc.Request
import uk.gov.hmrc.play.http.BadRequestException

import scala.concurrent.Future

trait AppointAgentController extends PropertyLinkingController {
  val representations = Wiring().propertyRepresentationConnector
  val properties = Wiring().propertyConnector
  val accounts = Wiring().groupAccountConnector
  val propertyLinks = Wiring().propertyLinkConnector
  val authenticated = Wiring().authenticated

  def add(linkId: String) = authenticated { implicit request =>
    representations.find(linkId).map(reprs => {
      if (reprs.nonEmpty)
        Ok(views.html.propertyRepresentation.alreadyAppointedAgent(SelectAgentVM(reprs, linkId)))
      else
        Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, linkId)))
    })
  }

  def edit(linkId: String) = authenticated { implicit request =>
    representations.find(linkId).map(reprs => {
      if (reprs.size > 1)
        Ok(views.html.propertyRepresentation.selectAgent(reprs))
      else {
        val form = appointAgentForm.fill(AppointAgent(reprs.head.agentId, reprs.head.canCheck, reprs.head.canChallenge))
        Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form, reprs.head.representationId)))
      }
    })
  }

  def select(linkId: String) = authenticated { implicit request =>
    representations.find(linkId).map(reprs => {
      Ok(views.html.propertyRepresentation.selectAgent(reprs))
    })
  }

  def appoint(linkId: String) = authenticated { implicit request =>
    Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, linkId)))
  }

  def appointSubmit(linkId: String) = authenticated.withAccounts { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, linkId))),
      agent => {
        for {
          link <- propertyLinks.get(linkId)
          l = link.getOrElse(throw new Exception(s"Invalid linkId $linkId"))
          account <- accounts.withAgentCode(agent.agentCode)
          prop <- properties.get(l.uarn)
          res <- (account, prop) match {
            case (Some(_), Some(_)) if agentHasNoPermissions(agent) =>
              val form = appointAgentForm.fill(agent).withError(invalidPermissions)
              invalidAppointment(form, linkId)
            case (None, Some(_)) if agentHasNoPermissions(agent) =>
              val form = appointAgentForm.fill(agent).withError(invalidAgentCode).withError(invalidPermissions)
              invalidAppointment(form, linkId)
            case (None, Some(p)) =>
              val form = appointAgentForm.fill(agent).withError(invalidAgentCode)
              invalidAppointment(form, linkId)
            case (Some(a), Some(p)) =>
              val req = PropertyRepresentation(java.util.UUID.randomUUID().toString, linkId, a.groupId, a.companyName, request.organisationAccount.id,
                request.organisationAccount.companyName, l.uarn, p.address, agent.canCheck, agent.canChallenge, true
              )
              representations.create(req) map { _ =>
                Ok(views.html.propertyRepresentation.appointedAgent(p.address, a.companyName))
              } recover {
                case _: BadRequestException => BadRequest(views.html.propertyRepresentation.invalidAppointment())
              }
            case _ => Future.successful(internalServerError)
          }
        } yield {
          res
        }
      }
    )
  }

  private def agentHasNoPermissions(a: AppointAgent) = a.canCheck == NotPermitted && a.canChallenge == NotPermitted

  private lazy val invalidPermissions = FormError("canCheck", "error.invalidPermissions")
  private lazy val invalidAgentCode = FormError("agentCode", "error.invalidAgentCode")

  private def invalidAppointment(form: Form[AppointAgent], linkId: String)(implicit request: Request[_]) = {
    Future.successful(BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(form, linkId))))
  }

  def modify(representationId: String) = authenticated { implicit request =>
    representations.get(representationId) map {
      case Some(rep) =>
        val form = appointAgentForm.fill(AppointAgent(rep.agentId, rep.canCheck, rep.canChallenge))
        Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form, rep.representationId)))
      case None => internalServerError
    }
  }

  def modifySubmit(representationId: String) = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(errors, representationId))),
      agent => {
        val updated = UpdatedRepresentation(representationId, agent.canCheck, agent.canChallenge)
        representations.update(updated) map { _ => Ok(views.html.propertyRepresentation.modifiedAgent()) }
      }
    )
  }

  lazy val appointAgentForm = Form(mapping(
    "agentCode" -> nonEmptyText,
    "canCheck" -> EnumMapping(AgentPermissions),
    "canChallenge" -> EnumMapping(AgentPermissions)
  )(AppointAgent.apply)(AppointAgent.unapply))
}

object AppointAgentController extends AppointAgentController

case class AppointAgent(agentCode: String, canCheck: AgentPermission, canChallenge: AgentPermission)

case class AppointAgentVM(form: Form[_], linkId: String)

case class ModifyAgentVM(form: Form[_], representationId: String)

case class SelectAgentVM(reps: Seq[PropertyRepresentation], linkId: String)
