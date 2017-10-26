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

import java.time._
import javax.inject.Inject

import actions.AuthenticatedAction
import com.builtamont.play.pdf.PdfGenerator
import config.{ApplicationConfig, Global}
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import models._
import models.messages.{Message, MessagePagination}
import models.searchApi.OwnerAuthResult
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import repositories.MessageCacheRepository

import scala.concurrent.Future

class Dashboard @Inject()(config: ApplicationConfig,
                          draftCases: DraftCases,
                          propertyLinks: PropertyLinkConnector,
                          messagesConnector: MessagesConnector,
                          authenticated: AuthenticatedAction,
                          messageCache: MessageCacheRepository,
                          pdfGen: PdfGenerator) extends PropertyLinkingController with ValidPagination {

  def home() = authenticated { implicit request =>
    if (request.organisationAccount.isAgent) {
      Redirect(controllers.agent.routes.RepresentationController.viewClientProperties())
    } else {
      Redirect(routes.Dashboard.manageProperties())
    }
  }

  def manageProperties(page: Int, pageSize: Int, requestTotalRowCount: Boolean = true) =
    managePropertiesSearchSort(page = page, pageSize = pageSize, requestTotalRowCount = requestTotalRowCount, None, None, None, None, None, None)

  def managePropertiesSearchSort(page: Int, pageSize: Int, requestTotalRowCount: Boolean = true, sortfield: Option[String] = None,
                                 sortorder: Option[String] = None, status: Option[String] = None, address: Option[String] = None,
                                 baref: Option[String] = None, agent: Option[String] = None) = authenticated { implicit request =>
    if (config.searchSortEnabled) {
      withValidPaginationSearchSort(
        page = page,
        pageSize = pageSize,
        requestTotalRowCount = requestTotalRowCount,
        sortfield = sortfield,
        sortorder = sortorder,
        status = status,
        address = address,
        baref = baref,
        agent = agent
      ) { paginationSearchSort =>
        for {
          response <- propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, paginationSearchSort)
          msgCount <- messagesConnector.countUnread(request.organisationId)
        } yield {
          Ok(views.html.dashboard.managePropertiesSearchSort(
            ManagePropertiesSearchAndSortVM(
              request.organisationAccount.id,
              response,
              paginationSearchSort.copy(totalResults = response.filterTotal)
            ),
            msgCount.unread
          ))
        }
      }
    } else {
      withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
        propertyLinks.linkedProperties(request.organisationId, pagination) map { response =>
          Ok(views.html.dashboard.manageProperties(
            ManagePropertiesVM(request.organisationAccount.id,
              response.propertyLinks,
              pagination.copy(totalResults = response.resultCount.getOrElse(0L)))))
        }
      }
    }
  }

  def getProperties(page: Int, pageSize: Int, requestTotalRowCount: Boolean) = authenticated { implicit request =>
    withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
      propertyLinks.linkedProperties(request.organisationId, pagination) map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  def getPropertiesSearchAndSort(page: Int,
                                 pageSize: Int,
                                 requestTotalRowCount: Boolean,
                                 sortfield: Option[String],
                                 sortorder: Option[String],
                                 status: Option[String],
                                 address: Option[String],
                                 baref: Option[String],
                                 agent: Option[String]) = authenticated { implicit request =>
    withValidPaginationSearchSort(page, pageSize, requestTotalRowCount, sortfield, sortorder, status, address, baref, agent) { pagination =>
      propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, pagination) map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  def manageAgents() = authenticated { implicit request =>
    for {
      response <- propertyLinks.linkedProperties(request.organisationId, Pagination(pageNumber = 1, pageSize = 100, resultCount = false))
      msgCount <- messagesConnector.countUnread(request.organisationId)
    } yield {
      val agentInfos = response.propertyLinks
        .flatMap(_.agents)
        .map(a => AgentInfo(a.organisationName, a.agentCode))
        .sortBy(_.organisationName).distinct
      Ok(views.html.dashboard.manageAgents(ManageAgentsVM(agentInfos), msgCount.unread))
    }
  }

  def viewManagedProperties(agentCode: Long) = authenticated { implicit request =>
    propertyLinks.linkedProperties(request.organisationId, Pagination(pageNumber = 1, pageSize = 100, resultCount = false)) map { response =>
      val filteredProps = response.propertyLinks.filter(_.agents.map(_.agentCode).contains(agentCode))
      if (filteredProps.nonEmpty) {
        val organisationName = filteredProps.flatMap(_.agents).filter(_.agentCode == agentCode).head.organisationName
        Ok(views.html.dashboard.managedByAgentsProperties(ManagedPropertiesVM(organisationName, agentCode, filteredProps)))
      }
      else
        NotFound(Global.notFoundTemplate)
    }
  }

  def viewDraftCases() = authenticated { implicit request =>
    for {
      cases <- draftCases.get(request.personId)
      msgCount <- messagesConnector.countUnread(request.organisationId)
    } yield {
      Ok(views.html.dashboard.draftCases(DraftCasesVM(cases), msgCount.unread))
    }
  }

  def viewMessages(pagination: MessagePagination) = authenticated { implicit request =>
    if(config.messagesEnabled) {
      withValidMessagePagination(pagination) {
        for {
          count <- messagesConnector.countUnread(request.organisationId)
          msgs <- messagesConnector.getMessages(request.organisationId, pagination)
          _ <- messageCache.saveOrUpdate(msgs.messages)
        } yield {
          //round up to nearest integer
          val numberOfPages: Int = Math.ceil(count.total.toDouble / pagination.pageSize).toInt
          Ok(views.html.dashboard.messagesTab(msgs, pagination, count.unread, numberOfPages))
        }
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def viewMessage(messageId: String) = authenticated { implicit request =>
    if(config.messagesEnabled) {
      for {
        message <- messageCache.getMessage(messageId)
        count <- messagesConnector.countUnread(request.organisationId)
        _ <- messagesConnector.markAsRead(messageId, request.individualAccount.externalId)
      } yield {
        message match {
          case Some(m) => Ok(views.html.dashboard.viewMessage(m, count.unread))
          case None => NotFound(Global.notFoundTemplate)
        }
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def viewMessageAsPdf(messageId: String) = authenticated { implicit request =>
    for {
      message <- messageCache.getMessage(messageId)
      count <- messagesConnector.countUnread(request.organisationId)
    } yield {
      message match {
        case Some(m) => pdfGen.ok(views.html.dashboard.viewMessagePdf(m), "localhost:9523")
        case None => NotFound(Global.notFoundTemplate)
      }
    }
  }

  private def withValidMessagePagination(pagination: MessagePagination)
                                        (f: => Future[Result])
                                        (implicit request: Request[_]): Future[Result] = {
    if (pagination.pageNumber >= 1 && pagination.pageSize >= 1 && pagination.pageSize <= 100) {
      f
    } else {
      BadRequest(Global.badRequestTemplate)
    }
  }
}

case class ManagePropertiesVM(organisationId: Long, properties: Seq[PropertyLink], pagination: Pagination)

case class ManagePropertiesSearchAndSortVM(organisationId: Long,
                                           result: OwnerAuthResult,
                                           pagination: PaginationSearchSort)


case class ManagedPropertiesVM(agentName: String, agentCode: Long, properties: Seq[PropertyLink])

case class ManageAgentsVM(agents: Seq[AgentInfo])

case class DraftCasesVM(draftCases: Seq[DraftCase])

case class PropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType, linkedDate: LocalDate,
                                       representations: Seq[PropertyRepresentation])

case class PendingPropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType,
                                              linkedDate: LocalDate, representations: Seq[PropertyRepresentation])

case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])

case class AgentInfo(organisationName: String, agentCode: Long)

case class ClientPropertiesVM(properties: Seq[ClientProperty])
