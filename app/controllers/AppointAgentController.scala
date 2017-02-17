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

import config.{ApplicationConfig, Global, Wiring}
import connectors.UpdatedRepresentation
import form.EnumMapping
import models._
import org.joda.time.DateTime
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.mvc.Request
import uk.gov.hmrc.play.http.BadRequestException
import views.html.propertyRepresentation.invalidAppointment

import scala.concurrent.Future

trait AppointAgentController extends PropertyLinkingController {
  val representations = Wiring().propertyRepresentationConnector
  val properties = Wiring().propertyConnector
  val accounts = Wiring().groupAccountConnector
  val propertyLinks = Wiring().propertyLinkConnector
  val authenticated = Wiring().authenticated

  def add(linkId: Long) = authenticated { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
      //representations.find(linkId).map(reprs => {
      //  if (reprs.nonEmpty)
      //    Ok(views.html.propertyRepresentation.alreadyAppointedAgent(SelectAgentVM(reprs, linkId)))
      //  else
          Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, linkId)))
      //})
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def edit(linkId: Long) = authenticated { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
      representations.find(linkId).map(reprs => {
        if (reprs.size > 1)
          Ok(views.html.propertyRepresentation.selectAgent(reprs))
        else {
          val form = appointAgentForm.fill(AppointAgent(/*FIXME*/ 123, reprs.head.checkPermission, reprs.head.challengePermission))
          Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form, reprs.head.representationId)))
        }
      })
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def select(linkId: Long) = authenticated { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
      representations.find(linkId).map(reprs => {
        Ok(views.html.propertyRepresentation.selectAgent(reprs))
      })
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def appoint(linkId: Long) = authenticated { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
      Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, linkId)))
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def appointSubmit(authorisationId: Long) = authenticated.withAccounts { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
      appointAgentForm.bindFromRequest().fold( errors => {
        BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, authorisationId)))
        }, agent => {
          val eventualAgentCodeResult = representations.validateAgentCode(agent.agentCode, authorisationId)
          val eventualMaybeLink = propertyLinks.get(request.organisationAccount.id, authorisationId)
          for {
            agentCodeValidationResult <- eventualAgentCodeResult
            propertyLink <- eventualMaybeLink
            res <- (agentCodeValidationResult, propertyLink) match {
              case (AgentCodeValidationResult(orgId, failureCode), Some(prop)) =>  {
                val codeError = failureCode.map {
                  case "INVALID_CODE" => invalidAgentCode
                  case "DUPLICATE_PARTY" => alreadyAppointedAgent
                }
                val permissionError = if (agentHasNoPermissions(agent)) Some(invalidPermissions) else None
                val errors: List[FormError] = List(codeError, permissionError).flatten
                if (errors.nonEmpty) {
                  val form = appointAgentForm.fill(agent)
                  val formWithErrors = errors.foldLeft(form){(f, error) => f.withError(error)}
                  invalidAppointment(formWithErrors, authorisationId)
                } else { val req = RepresentationRequest(authorisationId, agentCodeValidationResult.organisationId.getOrElse(-1),
                    request.individualAccount.individualId, java.util.UUID.randomUUID().toString,
                    agent.canCheck.name, agent.canChallenge.name, new DateTime())
                  representations.create(req) map { _ => Ok(views.html.propertyRepresentation.appointedAgent(prop.address)) }
                }
              }
              case (_, None) => {
                Future.successful(NotFound)
              }
            }
          } yield {
            res
          }
        }
      )
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  private def agentHasNoPermissions(a: AppointAgent) = a.canCheck == NotPermitted && a.canChallenge == NotPermitted

  private lazy val invalidPermissions = FormError("canCheck", "error.invalidPermissions")
  private lazy val invalidAgentCode = FormError("agentCode", "error.invalidAgentCode")
  private lazy val alreadyAppointedAgent = FormError("agentCode", "error.alreadyAppointedAgent")

  private def invalidAppointment(form: Form[AppointAgent], linkId: Long)(implicit request: Request[_]) = {
    Future.successful(BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(form, linkId))))
  }

  def modify(representationId: Long) = authenticated { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
      representations.get(representationId) map {
        case Some(rep) =>
          val form = appointAgentForm.fill(AppointAgent(/*FIXME*/123, rep.checkPermission, rep.challengePermission))
          Ok(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(form, rep.representationId)))
        case None => throw new Exception(s"Invalid representation id $representationId")
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def modifySubmit(representationId: Long) = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyRepresentation.modifyAgent(ModifyAgentVM(errors, representationId))),
      agent => {
        val updated = UpdatedRepresentation(representationId, agent.canCheck, agent.canChallenge)
        representations.update(updated) map { _ => Ok(views.html.propertyRepresentation.modifiedAgent()) }
      }
    )
  }

  lazy val appointAgentForm = Form(mapping(
    "agentCode" -> longNumber,
    "canCheck" -> EnumMapping(AgentPermission),
    "canChallenge" -> EnumMapping(AgentPermission)
  )(AppointAgent.apply)(AppointAgent.unapply))
}

object AppointAgentController extends AppointAgentController

case class AppointAgent(agentCode: Long, canCheck: AgentPermission, canChallenge: AgentPermission)

case class AppointAgentVM(form: Form[_], linkId: Long)

case class ModifyAgentVM(form: Form[_], representationId: Long)

case class SelectAgentVM(reps: Seq[PropertyRepresentation], linkId: Long)
