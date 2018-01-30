/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.agentAppointment

import java.time.Instant
import javax.inject.{Inject, Named}

import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import config.{ApplicationConfig, Global}
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import form.EnumMapping
import form.Mappings._
import models._
import models.searchApi.OwnerAgent
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.{FieldMapping, Form, FormError, Mapping}
import play.api.libs.json.Json
import play.api.mvc.Request
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.concurrent.Future

class AppointAgentController @Inject() (config: ApplicationConfig,
                                        representations: PropertyRepresentationConnector,
                                        accounts: GroupAccounts,
                                        propertyLinks: PropertyLinkConnector,
                                        agentsConnector: AgentsConnector,
                                        authenticated: AuthenticatedAction,
                                        @Named("agentAppointmentSession") val sessionRepository: SessionRepo)
  extends PropertyLinkingController {

  def appoint(linkId: Long) = authenticated { implicit request =>
    if (config.manageAgentsEnabled){
      agentsConnector.ownerAgents(request.organisationId) flatMap { ownerAgents =>
        sessionRepository.get[AgentAppointmentSession] flatMap {
          case Some(s) =>
            Ok(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(appointAgentForm.fill(s.agent), linkId, ownerAgents.agents)))
          case None =>
            Ok(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(appointAgentForm, linkId, ownerAgents.agents)))
        }
      }
    } else {
        sessionRepository.get[AgentAppointmentSession] flatMap {
          case Some(s) =>
            Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm.fill(s.agent), linkId)))
          case None =>
            Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, linkId)))
        }
    }
  }

  def appointSubmit(authorisationId: Long) = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(errors => {
      agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
        if (config.manageAgentsEnabled){
        BadRequest(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(errors, authorisationId, ownerAgents.agents)))
        } else {
          BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, authorisationId, ownerAgents.agents)))
        }}
    }, (agent: AppointAgent) => {
      val eventualAgentCodeResult = representations.validateAgentCode(agent.getAgentCode(), authorisationId)
      val eventualMaybeLink = propertyLinks.get(request.organisationAccount.id, authorisationId)
      for {
        agentCodeValidationResult <- eventualAgentCodeResult
        propertyLink <- eventualMaybeLink
        res <- (agentCodeValidationResult, propertyLink) match {
          case (AgentCodeValidationResult(_, failureCode), Some(prop)) => {
            val codeError = failureCode.map {
              case "INVALID_CODE" => invalidAgentCode
              case "DUPLICATE_PARTY" => alreadyAppointedAgent
            }
            val errors: List[FormError] = List(codeError).flatten
            if (errors.nonEmpty) {
              if (config.manageAgentsEnabled) {
                agentsConnector.ownerAgents(request.organisationId) flatMap { ownerAgents =>
                  val formWithErrors = errors.foldLeft(appointAgentForm.fill(agent)) { (f, error) => f.withError(error) }
                  invalidAppointment(formWithErrors, authorisationId, ownerAgents.agents)
                }
              } else {
                  val formWithErrors = errors.foldLeft(appointAgentForm.fill(agent)) { (f, error) => f.withError(error) }
                  invalidAppointment(formWithErrors, authorisationId)
              }
            } else {
              appointAgent(authorisationId, agent, agentCodeValidationResult.organisationId.getOrElse(-1L), prop)
            }
          }
          case (_, None) => Future.successful(NotFound(Global.notFoundTemplate)) //user entered a prop link manually that doesn't exist.
        }} yield {
        res
      }})
  }

  private def appointAgent(authorisationId: Long, agent: AppointAgent,
                           agentOrgId: Long, pLink: PropertyLink)(implicit request: BasicAuthenticatedRequest[_]) = {
    val hasCheckAgent = pLink.agents.map(_.checkPermission).contains(StartAndContinue)
    val hasChallengeAgent = pLink.agents.map(_.challengePermission).contains(StartAndContinue)
    if (hasCheckAgent && agent.canCheck == StartAndContinue || hasChallengeAgent && agent.canChallenge == StartAndContinue) {
      val agentSession = AgentAppointmentSession(agent, agentOrgId, pLink)
      sessionRepository.start[AgentAppointmentSession](agentSession).map(_ => {
        val permissions = pLink.agents.map(a => ExistingAgentsPermission(a.organisationName, a.agentCode,
          Seq(
            if (a.checkPermission == StartAndContinue) "check" else "",
            if (a.challengePermission == StartAndContinue) "challenge" else "").filter(_.nonEmpty)
        ))
        val newAgentPerms = ExistingAgentsPermission("", agent.getAgentCode(),
          Seq(
            if (agent.canChallenge == StartAndContinue) "challenge" else "",
            if (agent.canCheck == StartAndContinue) "check" else "").filter(_.nonEmpty)
        )
        Ok(views.html.propertyRepresentation.confirmAgentOverride(ConfirmOverrideVM(authorisationId, newAgentPerms, permissions)))
      })
    } else {
      for {
        _ <- createAndSubmitAgentRepRequest(authorisationId, agentOrgId, request.individualAccount.individualId, agent)
        _ <- sessionRepository.remove()
      } yield {
        Redirect(routes.AppointAgentController.appointed(authorisationId))
      }
    }
  }

  def appointed(authorisationId: Long) = authenticated { implicit request =>
    propertyLinks.get(request.organisationAccount.id, authorisationId) map {
      case Some(pl) => Ok(views.html.propertyRepresentation.appointedAgent(pl.address))
      case None => NotFound(Global.notFoundTemplate)
    }
  }

  private def updateAllAgentsPermission(authorisationId: Long, link: PropertyLink, newAgentPermission: AppointAgent,
                                        newAgentOrgId: Long, individualId: Long)(implicit hc: HeaderCarrier): Future[Unit] = {
    val updateExistingAgents = if (newAgentPermission.canCheck == StartAndContinue && newAgentPermission.canChallenge == StartAndContinue) {
      Future.sequence(link.agents.map(agent => representations.revoke(agent.authorisedPartyId)))
    } else if (newAgentPermission.canCheck == StartAndContinue) {
      val agentsToUpdate = link.agents.filter(_.checkPermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        //existing agents that had a check permission have been revoked
        //we now need to re-add the agents that had a challenge permission
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.challengePermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(authorisationId, agent.organisationId, individualId, NotPermitted, agent.challengePermission)
        })
      } yield {
        updatedAgents
      }
    } else {
      val agentsToUpdate = link.agents.filter(_.challengePermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.checkPermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(authorisationId, agent.organisationId, individualId, agent.checkPermission, NotPermitted)
        })
      } yield {
        updatedAgents
      }
    }
    updateExistingAgents.flatMap(_ => {
      //existing agents have been updated. Time to add the new agent.
      createAndSubmitAgentRepRequest(authorisationId, newAgentOrgId, individualId, newAgentPermission)
    })
  }

  private def createAndSubmitAgentRepRequest(authorisationId: Long, agentOrgId: Long, userIndividualId: Long,
                                             checkPermission: AgentPermission, challengePermission: AgentPermission)
                                            (implicit hc: HeaderCarrier): Future[Unit] = {
    val req = RepresentationRequest(authorisationId, agentOrgId, userIndividualId,
      java.util.UUID.randomUUID().toString, checkPermission.name, challengePermission.name, Instant.now)
    representations.create(req)
  }

  private def createAndSubmitAgentRepRequest(authorisationId: Long, agentOrgId: Long, userIndividualId: Long, appointedAgent: AppointAgent)
                                            (implicit hc: HeaderCarrier): Future[Unit] = {
    createAndSubmitAgentRepRequest(authorisationId, agentOrgId, userIndividualId, appointedAgent.canCheck, appointedAgent.canChallenge)
  }

  def confirmed(authorisationId: Long) = authenticated { implicit request =>
    sessionRepository.get[AgentAppointmentSession] flatMap {
      case Some(s) => {
        updateAllAgentsPermission(authorisationId, s.propertyLink, s.agent, s.agentOrgId, request.individualAccount.individualId)
          .map(_ => sessionRepository.remove())
          .map(_ => Ok(views.html.propertyRepresentation.appointedAgent(s.propertyLink.address)))
      }
      case None => NotFound(Global.notFoundTemplate)
    }
  }

  def declined(authorisationId: Long) = authenticated { implicit request =>
    sessionRepository.remove().map(_ => Redirect(controllers.routes.Dashboard.manageProperties()))
  }

  private lazy val invalidAgentCode = FormError("agentCode", "error.invalidAgentCode")
  private lazy val alreadyAppointedAgent = FormError("agentCode", "error.alreadyAppointedAgent")

  private def invalidAppointment(form: Form[AppointAgent], linkId: Long, agents: Seq[OwnerAgent] = Seq())(implicit request: Request[_]) = {
    if (config.manageAgentsEnabled){
      Future.successful(BadRequest(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(form, linkId, agents))))
    } else {
      Future.successful(BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(form, linkId, agents))))
    }
  }

  def appointAgentForm(implicit request: BasicAuthenticatedRequest[_]) = Form(mapping(
    "agentCode" -> mandatoryIfEqual("agentCodeRadio", "yes", agentCode)
          .verifying("error.selfAppointment", _ != request.organisationAccount.agentCode),
    "agentCodeRadio" -> text,
    "canCheck" -> AgentPermissionMapping("canChallenge"),
    "canChallenge" -> AgentPermissionMapping("canCheck")
  )(AppointAgent.apply)(AppointAgent.unapply))
}

case class AppointAgent(agentCode: Option[Long], agentCodeRadio: String, canCheck: AgentPermission, canChallenge: AgentPermission) {
  def getAgentCode(): Long = agentCode match {
    case Some(code) => code
    case None => agentCodeRadio.toLong
  }
}

object AppointAgent {
  implicit val format = Json.format[AppointAgent]
}

case class AppointAgentVM(form: Form[_], linkId: Long, agents: Seq[OwnerAgent] = Seq())

case class ModifyAgentVM(form: Form[_], representationId: Long)

case class ExistingAgentsPermission(agentName: String, agentCode: Long, availablePermission: Seq[String])

case class ConfirmOverrideVM(authorisationId: Long, newAgent: ExistingAgentsPermission, existingPermissions: Seq[ExistingAgentsPermission])

case class SelectAgentVM(reps: Seq[PropertyRepresentation], linkId: Long)

case class AgentPermissionMapping(other: String, key: String = "", constraints: Seq[Constraint[AgentPermission]] = Nil) extends Mapping[AgentPermission] {
  override val mappings = Seq(this)
  private val wrapped = EnumMapping(AgentPermission)

  override def bind(data: Map[String, String]) = {
    (wrapped.withPrefix(key).bind(data), wrapped.withPrefix(other).bind(data)) match {
      case (e@Left(err), _) => e
      case (Right(p1), Right(p2)) if p1 == NotPermitted && p2 == NotPermitted => Left(Seq(FormError(key, "error.invalidPermissions")))
      case (r@Right(_), _) => r
    }
  }

  override def unbind(value: AgentPermission) = {
    wrapped.withPrefix(key).unbind(value)
  }

  override def unbindAndValidate(value: AgentPermission) = {
    wrapped.withPrefix(key).unbindAndValidate(value)
  }

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(cs: Constraint[AgentPermission]*) = copy(constraints = constraints ++ cs.toSeq)
}
