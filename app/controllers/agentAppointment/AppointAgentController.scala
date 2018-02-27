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
import controllers._
import form.AgentPermissionMapping
import form.FormValidation.nonEmptyList
import form.Mappings._
import models._
import models.searchApi.{AgentPropertiesPagination, OwnerAgent, OwnerAuthResult}
import play.api.data.Forms.{number, _}
import play.api.data.{Form, FormError}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.concurrent.Future

class AppointAgentController @Inject() (representations: PropertyRepresentationConnector,
                                        accounts: GroupAccounts,
                                        propertyLinks: PropertyLinkConnector,
                                        agentsConnector: AgentsConnector,
                                        authenticated: AuthenticatedAction,
                                        @Named("agentAppointmentSession") val sessionRepository: SessionRepo)
                                       (implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with ValidPagination {

  def appoint(linkId: Long) = authenticated { implicit request =>
    if (config.manageAgentsEnabled) {
      agentsConnector.ownerAgents(request.organisationId) flatMap { ownerAgents =>
        sessionRepository.get[AgentAppointmentSession] flatMap {
          case Some(s) =>
            Ok(views.html.propertyRepresentation.appointAgentNew(
              AppointAgentVM(form = appointAgentForm.fill(s.agent),
                              linkId = Some(linkId),
                                agents = ownerAgents.agents)))
          case None =>
            Ok(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(appointAgentForm, Some(linkId), ownerAgents.agents)))
        }
      }
    } else {
      sessionRepository.get[AgentAppointmentSession] flatMap {
        case Some(s) =>
          Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm.fill(s.agent), Some(linkId))))
        case None =>
          Ok(views.html.propertyRepresentation.appointAgent(AppointAgentVM(appointAgentForm, Some(linkId))))
      }
    }
  }

  def appointSubmit(authorisationId: Long) = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(errors => {
      agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
        if (config.manageAgentsEnabled){
          BadRequest(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(errors, Some(authorisationId), ownerAgents.agents)))
        } else {
          BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, Some(authorisationId), ownerAgents.agents)))
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

  /* appoint agent to multiple properties - Start */
  def selectProperties() = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      hasErrors = errors => {
        agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
          if (config.manageAgentsEnabled) {
            BadRequest(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(errors, None, ownerAgents.agents)))
          } else {
            BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, None, ownerAgents.agents)))
          }
        }
      },
      success = (agent: AppointAgent) => {
        accounts.withAgentCode(agent.getAgentCode().toString) flatMap {
          case Some(group) => {
            val pagination = AgentPropertiesPagination(
              agentCode = agent.getAgentCode(),
              checkPermission = agent.canCheck,
              challengePermission = agent.canChallenge)
            for {
              response <- propertyLinks.appointableProperties(request.organisationId, pagination)
            } yield {
              Ok(views.html.propertyRepresentation.appointAgentProperties(None,
                AppointAgentPropertiesVM(group, response), pagination))
            }
          }
          case None => NotFound(s"Unknown Agent: ${agent.getAgentCode()}")
        }
      })
  }

  def selectPropertiesSearchSort(pagination: AgentPropertiesPagination) = authenticated { implicit request =>
    withValidPropertiesPagination(pagination) {
      accounts.withAgentCode(pagination.agentCode.toString) flatMap {
        case Some(group) => {
          for {
            response <- propertyLinks.appointableProperties(request.organisationId, pagination)
          } yield {
            Ok(views.html.propertyRepresentation.appointAgentProperties(None,
              AppointAgentPropertiesVM(group, response), pagination))
          }}
        case None => NotFound(s"Unknown Agent: ${pagination.agentCode}")
      }
    }
  }

  def appointAgentSummary() = authenticated { implicit request =>
    appointAgentBulkActionForm.bindFromRequest().fold(
      hasErrors = errors => {
        val data: Map[String, String] = errors.data
        val pagination = AgentPropertiesPagination(
          agentCode = data("agentCode").toLong,
          checkPermission = AgentPermission.fromName(data("checkPermission")).getOrElse(StartAndContinue),
          challengePermission = AgentPermission.fromName(data("challengePermission")).getOrElse(StartAndContinue))

        accounts.withAgentCode(pagination.agentCode.toString) flatMap {
          case Some(group) => {
            for {
              response <- propertyLinks.appointableProperties(request.organisationId, pagination)
            } yield BadRequest(views.html.propertyRepresentation.appointAgentProperties(Some(errors),
              AppointAgentPropertiesVM(group, response), pagination))
          }
          case None => NotFound(s"Unknown Agent: ${pagination.agentCode}")
        }
      },
      success = (action: AgentAppointBulkAction) => {
        accounts.withAgentCode(action.agentCode.toString) flatMap {
          case Some(group) => {
            for {
              // TODO get list/number of successful actions and pass to summary
              _ <- Future.traverse(action.propertyLinkIds)(pLink =>
                createAndSubmitAgentRepRequest(
                  pLink,
                  group.id,
                  request.organisationId,
                  request.individualAccount.individualId,
                  action.checkPermission,
                  action.challengePermission))
            } yield
              Ok(views.html.propertyRepresentation.appointAgentSummary(action, group.companyName))
          }
          case None => NotFound(s"Unknown Agent: ${action.agentCode}")
        }
      }
    )
  }

  private def createAndSubmitAgentRepRequest(pLink: String,
                                     agentOrgId: Long,
                                     organisationId: Long,
                                     individualId: Long,
                                     checkPermission: AgentPermission,
                                     challengePermission: AgentPermission)(implicit hc: HeaderCarrier): Future[Unit] = {

    propertyLinks.get(organisationId, pLink.toLong) map {
      case Some(prop) => updateAllAgentsPermission(
        pLink.toLong, prop,
        AppointAgent(None, "", checkPermission, challengePermission),
        agentOrgId,
        individualId)
      // shouldn't be possible for user to select a bad property link
      // just ignore if it does happen
      case None => Future.successful(Unit)
    }
  }

  /* appoint agent to multiple properties - End */


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
    if (config.manageAgentsEnabled) {
      Future.successful(BadRequest(views.html.propertyRepresentation.appointAgentNew(AppointAgentVM(form, Some(linkId), agents))))
    } else {
      Future.successful(BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(form, Some(linkId), agents))))
    }
  }

  def appointAgentForm(implicit request: BasicAuthenticatedRequest[_]) = Form(mapping(
    "agentCode" -> mandatoryIfEqual("agentCodeRadio", "yes",
      agentCode.verifying("error.selfAppointment", _ != request.organisationAccount.agentCode)),
    "agentCodeRadio" -> text,
    "canCheck" -> AgentPermissionMapping("canChallenge"),
    "canChallenge" -> AgentPermissionMapping("canCheck")
  )(AppointAgent.apply)(AppointAgent.unapply))

  def appointAgentBulkActionForm(implicit request: BasicAuthenticatedRequest[_]) = Form(mapping(
    "agentCode" -> longNumber,
    "checkPermission" -> text,
    "challengePermission" -> text,
    "linkIds" -> list(text).verifying(nonEmptyList)
  )(AgentAppointBulkAction.apply)(AgentAppointBulkAction.unpack _))


  private def withValidPropertiesPagination(pagination: AgentPropertiesPagination)
                                        (f: => Future[Result])
                                        (implicit request: Request[_]): Future[Result] = {
    if (pagination.pageNumber >= 1 && pagination.pageSize >= 1 && pagination.pageSize <= 1000) {
      f
    } else {
      BadRequest(Global.badRequestTemplate)
    }
  }

  def appointMultipleProperties() = authenticated { implicit request =>
      agentsConnector.ownerAgents(request.organisationId) flatMap { ownerAgents =>
        sessionRepository.get[AgentAppointmentSession] flatMap {
          case Some(s) =>
            Ok(views.html.propertyRepresentation.appointAgentNew(
              AppointAgentVM(form = appointAgentForm.fill(s.agent), agents = ownerAgents.agents)))
          case None =>
            Ok(views.html.propertyRepresentation.appointAgentNew(
              AppointAgentVM(form = appointAgentForm, agents = ownerAgents.agents)))
        }
      }
    }
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

case class AppointAgentPropertiesVM(agentGroup: GroupAccount, response: OwnerAuthResult)

case class AppointAgentVM(form: Form[_], linkId: Option[Long] = None, agents: Seq[OwnerAgent] = Seq())

case class ModifyAgentVM(form: Form[_], representationId: Long)

case class ExistingAgentsPermission(agentName: String, agentCode: Long, availablePermission: Seq[String])

case class ConfirmOverrideVM(authorisationId: Long, newAgent: ExistingAgentsPermission, existingPermissions: Seq[ExistingAgentsPermission])

case class SelectAgentVM(reps: Seq[PropertyRepresentation], linkId: Long)

object BulkActionsForm {
  lazy val form: Form[RepresentationBulkAction] = Form(mapping(
    "page" -> number,
    "pageSize" -> number,
    "action" -> text,
    "requestIds" -> list(text).verifying(nonEmptyList),
    "complete" -> optional(number)
  )(RepresentationBulkAction.apply)(RepresentationBulkAction.unapply))
}