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
import binders.GetPropertyLinksParameters
import binders.pagination.PaginationParameters
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
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.concurrent.{ExecutionContext, Future}

class AppointAgentController @Inject()(
                                        representations: PropertyRepresentationConnector,
                                        accounts: GroupAccounts,
                                        propertyLinks: PropertyLinkConnector,
                                        agentsConnector: AgentsConnector,
                                        authenticated: AuthenticatedAction
                                      )(implicit val messagesApi: MessagesApi, executionContext: ExecutionContext, val config: ApplicationConfig)
  extends PropertyLinkingController with ValidPagination {

  val logger: Logger = Logger(this.getClass)

  def getMyOrganisationPropertyLinks(
                                      pagination: PaginationParameters,
                                      params: GetPropertyLinksParameters,
                                      agentCode: Long,
                                      checkPermission: String,
                                      challengePermission: String
                                      //agentAppointed: String commenting this out but could have one method for everything
                                    ): Action[AnyContent] = authenticated { implicit request =>
    for {
      agentOrganisation <- accounts.withAgentCode(agentCode.toString)
      response <- propertyLinks.linkedPropertiesSearchAndSort(params, PaginationParams(startPoint = pagination.startPoint, pageSize = pagination.pageSize, requestTotalRowCount = false), ownerOrAgent = OwnerOrAgent.OWNER)
    } yield {
      agentOrganisation match {
        case Some(organisation) =>
          Ok(views.html.propertyrepresentation.appointAgentProperties(
            f = None,
            model = AppointAgentPropertiesVM(organisation, response, Some(agentCode), AgentPermission.fromName(checkPermission), AgentPermission.fromName(challengePermission), true),
            pagination = pagination,
            params = params,
            agentCode = agentCode,
            checkPermission = checkPermission,
            challengePermission = challengePermission
          ))
        case None =>
          notFound
      }
    }
  }

  //TODO needs to call method that actually strips agent.
  def selectPropertiesWithNoAgent(
                                   pagination: PaginationParameters,
                                   params: GetPropertyLinksParameters,
                                   agentCode: Long,
                                   checkPermission: String,
                                   challengePermission: String,
                                   agentAppointed: String
                                 ): Action[AnyContent] = authenticated { implicit request =>
    accounts.withAgentCode(agentCode.toString) flatMap {
      case Some(group) =>
        for {
          response <- propertyLinks.linkedPropertiesSearchAndSort(
            GetPropertyLinksParameters(
              address = params.address,
              sortfield = params.sortfield,
              sortorder = params.sortorder
            ),
            PaginationParams(
              startPoint = pagination.startPoint,
              pageSize = pagination.pageSize,
              requestTotalRowCount = false
            ),
            ownerOrAgent = OwnerOrAgent.OWNER)
        } yield {
          Ok(views.html.propertyrepresentation.appointAgentProperties(
            None,
            AppointAgentPropertiesVM(group, response, Some(agentCode), AgentPermission.fromName(checkPermission), AgentPermission.fromName(challengePermission), false),
            pagination,
            params,
            agentCode,
            checkPermission,
            challengePermission))
        }
      case None =>
        logger.warn("agent code was not found in modernised")
        //Throwing exception here until we can get a better idea what error we should throw.
        throw new IllegalArgumentException("agent code was not found in modernised")
    }
  }


  def selectAgentProperties() = authenticated { implicit request =>
    registeredAgentForm.bindFromRequest().fold(
      hasErrors = errors => {
        agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
          BadRequest(views.html.propertyrepresentation.loadAgentsForRemove(AppointAgentVM(errors, None, ownerAgents.agents)))
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
                response <- propertyLinks.linkedPropertiesSearchAndSort(GetPropertyLinksParameters(agent = Some(group.companyName), sortfield = Some(pagination.sortField.name), sortorder = Some(pagination.sortOrder.name)), PaginationParams(1, 1000, false), ownerOrAgent = OwnerOrAgent.AGENT)
              } yield {

                Ok(views.html.propertyrepresentation.revokeAgentProperties(None,
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
              response <- if (request.organisationAccount.isAgent) {
                propertyLinks.linkedPropertiesSearchAndSort(GetPropertyLinksParameters(address = pagination.address,
                  agent = Some(group.companyName),
                  sortfield = Some(pagination.sortField.name),
                  sortorder = Some(pagination.sortOrder.name)),
                  PaginationParams(pagination.pageNumber, pagination.pageSize, false), ownerOrAgent = OwnerOrAgent.AGENT)
                  .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                  .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                  .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
              }
              else {
                propertyLinks.linkedPropertiesSearchAndSort(GetPropertyLinksParameters(address = pagination.address,
                  sortfield = Some(pagination.sortField.name),
                  sortorder = Some(pagination.sortOrder.name)),
                  PaginationParams(pagination.pageNumber, pagination.pageSize, false), ownerOrAgent = OwnerOrAgent.OWNER)
                  .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                  .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                  .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
              }
            } yield {
              Ok(views.html.propertyrepresentation.revokeAgentProperties(None,
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
            } yield BadRequest(views.html.propertyrepresentation.appointAgentProperties(Some(errors), AppointAgentPropertiesVM(group, response), PaginationParameters(), GetPropertyLinksParameters(), data("agentCode").toLong, data("checkPermission"), data("challengePermission")))
          }
          case None =>
            notFound
        }
      },
      success = (action: AgentAppointBulkAction) => {
        Logger.debug(s"the request does indeed come here ${action}")
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
                  action.challengePermission,
                  request.organisationAccount.isAgent)).recover {
                case e => Logger.warn(s"Failed to get a property link during multiple property agent appointment: ${e.getMessage}", e)
              }
            } yield
              Ok(views.html.propertyrepresentation.appointAgentSummary(action, group.companyName))
          }
          case None =>
            notFound
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
                response <- if (request.organisationAccount.isAgent) {
                  propertyLinks.linkedPropertiesSearchAndSort(GetPropertyLinksParameters(address = pagination.address,
                    agent = Some(group.companyName),
                    sortfield = Some(pagination.sortField.name),
                    sortorder = Some(pagination.sortOrder.name)),
                    PaginationParams(pagination.pageNumber, pagination.pageSize, false), ownerOrAgent = OwnerOrAgent.AGENT)
                    .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                    .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                    .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
                }
                else {
                  propertyLinks.linkedPropertiesSearchAndSort(GetPropertyLinksParameters(address = pagination.address,
                    sortfield = Some(pagination.sortField.name),
                    sortorder = Some(pagination.sortOrder.name)),
                    PaginationParams(pagination.pageNumber, pagination.pageSize, false), ownerOrAgent = OwnerOrAgent.OWNER)
                    .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                    .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                    .map(oar => oar.copy(authorisations = oar.authorisations.take(15)))
                }
              } yield {
                BadRequest(views.html.propertyrepresentation.revokeAgentProperties(Some(errors),
                  AppointAgentPropertiesVM(group, response), pagination))
              }
            }
          }
          case None =>
            notFound
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
                  action.agentCode,
                  request.organisationAccount.isAgent)).recover {
                case e => Logger.warn(s"Failed to get a property link during revoke multiple property agent: ${e.getMessage}", e)
              }
            } yield
              Ok(views.html.propertyrepresentation.revokeAgentSummary(action, group.companyName))
          }
          case None =>
            notFound
        }
      }
    )
  }

  def filterProperties(authorisations: Seq[OwnerAuthorisation], agentId: Long) = {
    authorisations.filter(auth =>
      Seq(PropertyLinkingApproved.name, PropertyLinkingPending.name).contains(auth.status))
      .filter(_.agents.fold(false)(_.map(_.organisationId).exists(id => (agentId == id))))
  }

  def getAgentsForRemove() = authenticated { implicit request =>
    agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
      Ok(views.html.propertyrepresentation.loadAgentsForRemove(
        AppointAgentVM(form = registeredAgentForm, agents = ownerAgents.agents)))
    }
  }

  def registeredAgentForm(implicit request: BasicAuthenticatedRequest[_]) = Form(mapping(
    "agentCodeRadio" -> text
  )(AgentId.apply)(AgentId.unapply))


  private def createAndSubmitAgentRepRequest(
                                              pLink: String,
                                              agentOrgId: Long,
                                              organisationId: Long,
                                              individualId: Long,
                                              checkPermission: AgentPermission,
                                              challengePermission: AgentPermission,
                                              isAgent: Boolean
                                            )(implicit hc: HeaderCarrier): Future[Unit] = {
    val link = propertyLinks.getOwnerAssessments(pLink)

    logger.warn(s"something happens here $link")
    link map {
      case Some(prop) => {
        updateAllAgentsPermission(
          prop.authorisationId,
          prop,
          AppointAgent(None, "", checkPermission, challengePermission),
          agentOrgId,
          individualId,
          organisationId)
      }
        logger.warn(s"User has selected a bad property submission ID $pLink - this shouldn't be possible. 1 ")
      // just ignore if it does happen
      case None =>
        logger.warn(s"User has selected a bad property submission ID $pLink - this shouldn't be possible. w")
        Future.successful(Unit)
    }
  }


  private def createAndSubitAgentRevokeRequest(pLink: String,
                                               organisationId: Long,
                                               agentCode: Long,
                                               isAgent: Boolean)(implicit hc: HeaderCarrier): Future[Unit] = {

    val link = propertyLinks.getOwnerAssessments(pLink)

    link flatMap {
      case Some(link) => link.agents.find(a => a.agentCode == agentCode) match {
        case Some(agent) => representations.revoke(agent.authorisedPartyId)
          logger.warn(s"Agent $agentCode does not exist for the property link with subission ID $pLink - this shouldn't be possible.")
        // shouldn't be possible for agent not to exist in property links
        // just ignore if it does happen
        case None =>
          logger.warn(s"User has selected a bad property submission ID $pLink - this shouldn't be possible. 1")
          Future.successful(Unit)
      }
        logger.warn(s"User has selected a bad property submission ID $pLink - this shouldn't be possible. 2")
      // just ignore if it does happen
      case None =>
        logger.warn(s"User has selected a bad property submission ID $pLink - this shouldn't be possible. 3")
        Future.successful(Unit)
    }
  }

  private def updateAllAgentsPermission(
                                         authorisationId: Long,
                                         link: ApiAssessments,
                                         newAgentPermission: AppointAgent,
                                         newAgentOrgId: Long,
                                         individualId: Long,
                                         organisationId: Long
                                       )(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.warn("it comes in here")
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
    Future.successful(BadRequest(views.html.propertyrepresentation.appointAgent(AppointAgentVM(form, linkId, agents),
      Some(config.newDashboardUrl("your-agents")))))
  }

  private def invalidRevokeAppointment(form: Form[AgentId], linkId: Option[Long], agents: Seq[OwnerAgent] = Seq())(implicit request: Request[_]) = {
    Future.successful(BadRequest(views.html.propertyrepresentation.loadAgentsForRemove(AppointAgentVM(form, linkId, agents))))
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
      Ok(views.html.propertyrepresentation.appointAgent(
        AppointAgentVM(form = appointAgentForm, agents = ownerAgents.agents), Some(config.newDashboardUrl("your-agents"))))
    }
  }


  def submitAppointMultipleProperties(): Action[AnyContent] = authenticated { implicit request =>
    appointAgentForm.bindFromRequest().fold(
      hasErrors = errors => {
        agentsConnector.ownerAgents(request.organisationId) map { ownerAgents =>
          BadRequest(views.html.propertyrepresentation.appointAgent(AppointAgentVM(errors, None, ownerAgents.agents),
            Some(config.newDashboardUrl("your-agents"))))
        }
      },
      success = (agent: AppointAgent) => {
        accounts.withAgentCode(agent.getAgentCode().toString) flatMap {
          case Some(group) =>
            Future.successful(Redirect(routes.AppointAgentController.getMyOrganisationPropertyLinks(PaginationParameters(), GetPropertyLinksParameters(), agent.getAgentCode(), agent.canCheck.name, agent.canChallenge.name)))
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
}

case class AppointAgentPropertiesVM(agentGroup: GroupAccount,
                                    response: OwnerAuthResult,
                                    agentCode: Option[Long] = None,
                                    checkPermission: Option[AgentPermission] = None,
                                    challengePermission: Option[AgentPermission] = None,
                                    showAllProperties: Boolean = false)

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