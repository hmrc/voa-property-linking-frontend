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

import javax.inject.Inject

import actions.AgentRequest
import config.{ApplicationConfig, Global, Wiring}
import connectors.DraftCases
import models._
import models.searchApi.OwnerAuthResult
import org.joda.time.DateTime
import play.api.libs.json.Json

class Dashboard @Inject()(draftCases: DraftCases) extends PropertyLinkingController with ValidPagination {
  val propertyLinks = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val auth = Wiring().authConnector
  val authenticated = Wiring().authenticated

  def home() = authenticated { implicit request =>
    request.organisationAccount.isAgent match {
      case true => Redirect(controllers.agent.routes.RepresentationController.viewClientProperties())
      case false => Redirect(routes.Dashboard.manageProperties())
    }
  }

  def manageProperties(page: Int, pageSize: Int) = authenticated { implicit request =>
    withValidPagination(page, pageSize) { pagination =>

      if(ApplicationConfig.searchSortEnabled) {
          propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, pagination) map { response =>
            Ok(views.html.dashboard.managePropertiesSearchSort(
              ManagePropertiesSearchAndSortVM(request.organisationAccount.id,
                response,
                pagination.copy(
                  totalResults = response.total))))
          }
        } else {
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
                                 agent: Option[String])  = authenticated { implicit request =>
    withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
      propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, pagination, sortfield, sortorder, status, address, baref, agent) map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  def manageAgents() = authenticated { implicit request =>
    for {
      response <- propertyLinks.linkedProperties(request.organisationId, Pagination(pageNumber = 1, pageSize = 100, resultCount = false))
    } yield {
      val agentInfos = response.propertyLinks
        .flatMap(_.agents)
        .map(a => AgentInfo(a.organisationName, a.agentCode))
        .sortBy(_.organisationName).distinct
      Ok(views.html.dashboard.manageAgents(ManageAgentsVM(agentInfos)))
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
    if (ApplicationConfig.casesEnabled) {
      draftCases.get(request.personId) map { cases =>
        Ok(views.html.dashboard.draftCases(DraftCasesVM(cases)))
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }
}

case class ManagePropertiesVM(organisationId: Long, properties: Seq[PropertyLink], pagination: Pagination)
case class ManagePropertiesSearchAndSortVM(organisationId: Long, result: OwnerAuthResult, pagination: Pagination)


case class ManagedPropertiesVM(agentName: String, agentCode: Long, properties: Seq[PropertyLink])

case class ManageAgentsVM(agents: Seq[AgentInfo])

case class DraftCasesVM(draftCases: Seq[DraftCase])

case class PropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType, linkedDate: DateTime,
                                       representations: Seq[PropertyRepresentation])

case class PendingPropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType,
                                              linkedDate: DateTime, representations: Seq[PropertyRepresentation])

case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])

case class AgentInfo(organisationName: String, agentCode: Long)

case class ClientPropertiesVM(properties: Seq[ClientProperty])
