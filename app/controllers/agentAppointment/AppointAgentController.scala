/*
 * Copyright 2019 HM Revenue & Customs
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

import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import auditing.AuditingService
import config.{ApplicationConfig, Global}
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import controllers._
import form.AgentPermissionMapping
import form.FormValidation.nonEmptyList
import form.Mappings._
import javax.inject.Inject
import models._
import models.searchApi._
import play.api.Logger
import play.api.data.Forms.{number, _}
import play.api.data.{Form, FormError}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.concurrent.Future

class AppointAgentController @Inject()(representations: PropertyRepresentationConnector,
                                       accounts: GroupAccounts,
                                       propertyLinks: PropertyLinkConnector,
                                       agentsConnector: AgentsConnector,
                                       authenticated: AuthenticatedAction)
                                      (implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with ValidPagination {

  def selectAgentProperties() = authenticated { implicit request =>
    registeredAgentForm.bindFromRequest().fold(
      hasErrors = errors => {
        agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
          BadRequest(views.html.propertyRepresentation.loadAgentsForRemove(AppointAgentVM(errors, None, ownerAgents.agents)))
        }
      },
      success = (agent: AgentId) => {
        accounts.withAgentCode(agent.id) flatMap {
          case Some(group) => {
            val pagination = AgentPropertiesParameters(agentCode = agent.id.toLong)

            withValidPaginationSearchSort(
              page = 1,
              pageSize = 1000,
              agent = Some(group.companyName),
              sortfield = Some(pagination.sortField.name),
              sortorder = Some(pagination.sortOrder.name)
            ) { paginationSearchSort =>

              for {
                response <- propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, paginationSearchSort)
                  .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                  .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                  .map(oar => oar.copy(authorisations = oar.authorisations.take(15)))

              } yield {

                Ok(views.html.propertyRepresentation.revokeAgentProperties(None,
                  AppointAgentPropertiesVM(group, response), pagination))
              }
            }
          }
          case None => {
            val errors: List[FormError] = List(invalidAgentCode)
            agentsConnector.ownerAgents(request.organisationId) flatMap { ownerAgents =>
              val formWithErrors = errors.foldLeft(registeredAgentForm.fill(AgentId(agent.id))) { (f, error) => f.withError(error) }
              invalidRevokeAppointment(formWithErrors, None, ownerAgents.agents)
            }
          }
        }
      })
  }

  def selectProperties() = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      hasErrors = errors => {
        agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
          BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(errors, None, ownerAgents.agents),
            Some(config.newDashboardUrl("your-agents"))))
        }
      },
      success = (agent: AppointAgent) => {
        accounts.withAgentCode(agent.getAgentCode().toString) flatMap {
          case Some(group) => {
            val pagination = AgentPropertiesParameters(
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
          case None => {
            val errors: List[FormError] = List(invalidAgentCode)
            agentsConnector.ownerAgents(request.organisationId) flatMap { ownerAgents =>
              val formWithErrors = errors.foldLeft(appointAgentForm.fill(agent)) { (f, error) => f.withError(error) }
              invalidAppointment(formWithErrors, None, ownerAgents.agents)
            }
          }
        }
      })
  }

  def selectPropertiesSearchSort(pagination: AgentPropertiesParameters) = authenticated { implicit request =>
    withValidPropertiesPagination(pagination) {
      accounts.withAgentCode(pagination.agentCode.toString) flatMap {
        case Some(group) => {
          for {
            response <- propertyLinks.appointableProperties(request.organisationId, pagination)
          } yield {
            Ok(views.html.propertyRepresentation.appointAgentProperties(None,
              AppointAgentPropertiesVM(group, response), pagination))
          }
        }
        case None => NotFound(s"Unknown Agent: ${pagination.agentCode}")
      }
    }
  }

  def selectAgentPropertiesSearchSort(pagination: AgentPropertiesParameters) = authenticated { implicit request =>
    withValidPropertiesPagination(pagination) {
      accounts.withAgentCode(pagination.agentCode.toString) flatMap {
        case Some(group) => {
          withValidPaginationSearchSort(
            page = 1,
            pageSize = 1000,
            address = pagination.address,
            agent = Some(group.companyName),
            sortfield = Some(pagination.sortField.name),
            sortorder = Some(pagination.sortOrder.name)
          ) { paginationSearchSort =>

            for {
              response <- propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, paginationSearchSort)
                .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
            } yield {
              Ok(views.html.propertyRepresentation.revokeAgentProperties(None,
                AppointAgentPropertiesVM(group, response), pagination))
            }
          }
        }
        case None => NotFound(s"Unknown Agent: ${pagination.agentCode}")
      }
    }
  }

  def appointAgentSummary() = authenticated { implicit request =>
    appointAgentBulkActionForm.bindFromRequest().fold(
      hasErrors = errors => {
        val data: Map[String, String] = errors.data
        val pagination = AgentPropertiesParameters(
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
              _ <- Future.traverse(action.propertyLinkIds)(pLink =>
                createAndSubmitAgentRepRequest(
                  pLink,
                  group.id,
                  request.organisationId,
                  request.individualAccount.individualId,
                  action.checkPermission,
                  action.challengePermission)).recover {
                case e => Logger.info(s"Failed to get a property link during multiple property agent appointment: ${e.getMessage}")
              }
            } yield
              Ok(views.html.propertyRepresentation.appointAgentSummary(action, group.companyName))
          }
          case None => NotFound(s"Unknown Agent: ${action.agentCode}")
        }
      }
    )
  }

  def revokeAgentSummary() = authenticated { implicit request =>
    revokeAgentBulkActionForm.bindFromRequest().fold(
      hasErrors = errors => {
        val data: Map[String, String] = errors.data
        val pagination = AgentPropertiesParameters(
          agentCode = data("agentCode").toLong)

        accounts.withAgentCode(pagination.agentCode.toString) flatMap {
          case Some(group) => {
            withValidPaginationSearchSort(
              page = 1,
              pageSize = 1000,
              agent = Some(group.companyName),
              sortfield = Some(pagination.sortField.name),
              sortorder = Some(pagination.sortOrder.name)
            ) { paginationSearchSort =>

              for {
                response <- propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, paginationSearchSort)
                  .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                  .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                  .map(oar => oar.copy(authorisations = oar.authorisations.take(15)))
              } yield {
                BadRequest(views.html.propertyRepresentation.revokeAgentProperties(Some(errors),
                  AppointAgentPropertiesVM(group, response), pagination))
              }
            }
          }
          case None => NotFound(s"Unknown Agent: ${pagination.agentCode}")
        }
      },
      success = (action: AgentRevokeBulkAction) => {
        accounts.withAgentCode(action.agentCode.toString) flatMap {
          case Some(group) => {
            for {

              _ <- Future.traverse(action.propertyLinkIds)(pLink =>
                createAndSubitAgentRevokeRequest(
                  pLink,
                  request.organisationId,
                  action.agentCode)).recover {
                case e => Logger.info(s"Failed to get a property link during revoke multiple property agent: ${e.getMessage}")
              }
            } yield
              Ok(views.html.propertyRepresentation.revokeAgentSummary(action, group.companyName))
          }
          case None => NotFound(s"Unknown Agent: ${action.agentCode}")
        }
      }
    )
  }

  def filterProperties(authorisations: Seq[OwnerAuthorisation], agentId: Long)  = {
    authorisations.filter(auth =>
      Seq(PropertyLinkingApproved.name, PropertyLinkingPending.name).contains(auth.status))
      .filter(_.agents.fold(false)(_.map(_.organisationId).exists(id => (agentId == id))))
  }


  def getAgentsForRemove() = authenticated { implicit request =>
    agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
      Ok(views.html.propertyRepresentation.loadAgentsForRemove(
        AppointAgentVM(form = registeredAgentForm, agents = ownerAgents.agents)))
    }
  }

  def registeredAgentForm(implicit request: BasicAuthenticatedRequest[_]) = Form(mapping(
    "agentCodeRadio" -> text
  )(AgentId.apply)(AgentId.unapply))


  private def createAndSubmitAgentRepRequest(pLink: String,
                                             agentOrgId: Long,
                                             organisationId: Long,
                                             individualId: Long,
                                             checkPermission: AgentPermission,
                                             challengePermission: AgentPermission)(implicit hc: HeaderCarrier): Future[Unit] = {

    propertyLinks.get(organisationId, pLink.toLong) map {
      case Some(prop) => {
        updateAllAgentsPermission(
          pLink.toLong, prop,
          AppointAgent(None, "", checkPermission, challengePermission),
          agentOrgId,
          individualId,
          organisationId)
      }
      // shouldn't be possible for user to select a bad property link
      // just ignore if it does happen
      case None => Future.successful(Unit)
    }
  }


  private def createAndSubitAgentRevokeRequest(pLink: String,
                                               organisationId: Long,
                                               agentCode: Long)(implicit hc: HeaderCarrier): Future[Unit] = {


    propertyLinks.get(organisationId, pLink.toLong) flatMap {
      case Some(link) => link.agents.find(a => a.agentCode == agentCode) match {
        case Some(agent) => {
          representations.revoke(agent.authorisedPartyId)
        }
      }
    }

  }

  private def updateAllAgentsPermission(authorisationId: Long, link: PropertyLink, newAgentPermission: AppointAgent,
                                        newAgentOrgId: Long, individualId: Long, organisationId: Long)(implicit hc: HeaderCarrier): Future[Unit] = {
    val updateExistingAgents = if (newAgentPermission.canCheck == StartAndContinue && newAgentPermission.canChallenge == StartAndContinue) {
      Future.sequence(link.agents.map(agent => representations.revoke(agent.authorisedPartyId)))
    } else if (newAgentPermission.canCheck == StartAndContinue) {
      val agentsToUpdate = link.agents.filter(_.checkPermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        //existing agents that had a check permission have been revoked
        //we now need to re-add the agents that had a challenge permission
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.challengePermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(authorisationId, agent.organisationId, individualId, NotPermitted, agent.challengePermission, organisationId)
        })
      } yield {
        updatedAgents
      }
    } else {
      val agentsToUpdate = link.agents.filter(_.challengePermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.checkPermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(authorisationId, agent.organisationId, individualId, agent.checkPermission, NotPermitted, organisationId)
        })
      } yield {
        updatedAgents
      }
    }
    updateExistingAgents.flatMap(_ => {
      //existing agents have been updated. Time to add the new agent.
      createAndSubmitAgentRepRequest(authorisationId, newAgentOrgId, individualId, newAgentPermission, organisationId)
    })
  }

  private def createAndSubmitAgentRepRequest(authorisationId: Long, agentOrgId: Long, userIndividualId: Long,
                                             checkPermission: AgentPermission, challengePermission: AgentPermission, organisationId: Long)
                                            (implicit hc: HeaderCarrier): Future[Unit] = {
    val submissionId = java.util.UUID.randomUUID().toString
    val createDatetime = Instant.now
    val req = RepresentationRequest(authorisationId, agentOrgId, userIndividualId,
      submissionId, checkPermission.name, challengePermission.name, createDatetime)

    representations.create(req).map(x => {
      AuditingService.sendEvent("agent representation request approve", Json.obj(
        "organisationId" -> organisationId,
        "individualId" -> userIndividualId,
        "propertyLinkId" -> authorisationId,
        "agentOrganisationId" -> agentOrgId,
        "submissionId" -> submissionId,
        "checkPermission" -> checkPermission.name,
        "challengePermission" -> challengePermission.name,
        "createDatetime" -> createDatetime.toString
      ))
    })
  }


  private def createAndSubmitAgentRepRequest(authorisationId: Long, agentOrgId: Long, userIndividualId: Long, appointedAgent: AppointAgent, organisationId: Long)
                                            (implicit hc: HeaderCarrier): Future[Unit] = {
    createAndSubmitAgentRepRequest(authorisationId, agentOrgId, userIndividualId, appointedAgent.canCheck, appointedAgent.canChallenge, organisationId)
  }

  private lazy val invalidAgentCode = FormError("agentCode", "error.invalidAgentCode")
  private lazy val alreadyAppointedAgent = FormError("agentCode", "error.alreadyAppointedAgent")

  private def invalidAppointment(form: Form[AppointAgent], linkId: Option[Long], agents: Seq[OwnerAgent] = Seq())(implicit request: Request[_]) = {
    Future.successful(BadRequest(views.html.propertyRepresentation.appointAgent(AppointAgentVM(form, linkId, agents),
      Some(config.newDashboardUrl("your-agents")))))
  }

  private def invalidRevokeAppointment(form: Form[AgentId], linkId: Option[Long], agents: Seq[OwnerAgent] = Seq())(implicit request: Request[_]) = {
    Future.successful(BadRequest(views.html.propertyRepresentation.loadAgentsForRemove(AppointAgentVM(form, linkId, agents))))
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

  def revokeAgentBulkActionForm(implicit request: BasicAuthenticatedRequest[_]) = Form(mapping(
    "agentCode" -> longNumber,
    "linkIds" -> list(text).verifying(nonEmptyList)
  )(AgentRevokeBulkAction.apply)(AgentRevokeBulkAction.unpack _))

  private def withValidPropertiesPagination(pagination: AgentPropertiesParameters)
                                           (f: => Future[Result])
                                           (implicit request: Request[_]): Future[Result] = {
    if (pagination.pageNumber >= 1 && pagination.pageSize >= 1 && pagination.pageSize <= 1000) {
      f
    } else {
      BadRequest(Global.badRequestTemplate)
    }
  }

  def appointMultipleProperties() = authenticated { implicit request =>
    agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
      Ok(views.html.propertyRepresentation.appointAgent(
        AppointAgentVM(form = appointAgentForm, agents = ownerAgents.agents), Some(config.newDashboardUrl("your-agents"))))
    }
  }

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