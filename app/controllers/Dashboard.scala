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
import controllers.Application.withThrottledHoldingPage
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

trait Dashboard extends PropertyLinkingController {
  val propertyLinks = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val auth = Wiring().authConnector
  val authenticated = Wiring().authenticated

  def home() = authenticated { implicit request =>
    withThrottledHoldingPage("dashboard", Ok(views.html.errors.errorDashboard())) {
      Ok(views.html.dashboard.home(request.individualAccount.details, request.organisationAccount))
    }
  }

  def manageProperties(page: Int, pageSize: Int) = authenticated { implicit request =>
    withValidPagination(page, pageSize) {
      propertyLinks.linkedProperties(request.organisationId, Pagination.getStartPoint(page, pageSize), pageSize, requestTotalRowCount = true) map { response =>
        val pagination = Pagination(page, pageSize, response.resultCount.getOrElse(0))
        Ok(views.html.dashboard.manageProperties(ManagePropertiesVM(request.individualAccount.organisationId, response.propertyLinks, pagination)))
      }
    }
  }

  def getProperties(page: Int, pageSize: Int, requestTotalRowCount: Boolean) = authenticated { implicit request =>
    withValidPagination(page, pageSize) {
      propertyLinks.linkedProperties(request.organisationId, Pagination.getStartPoint(page, pageSize), pageSize, requestTotalRowCount) map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  private def withValidPagination(page: Int, pageSize: Int)(default: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (page <= 0 || pageSize < 10 || pageSize > 100) {
      BadRequest(Global.badRequestTemplate)
    } else {
      default
    }
  }

  def manageAgents() = authenticated { implicit request =>
    for {
      response <- propertyLinks.linkedProperties(request.organisationId, 1, 100, requestTotalRowCount = false)
    } yield {
      val agentInfos = response.propertyLinks
        .flatMap(_.agents)
        .map(a=> AgentInfo(a.organisationName, a.agentCode))
        .sortBy(_.organisationName).distinct
      Ok(views.html.dashboard.manageAgents(ManageAgentsVM(agentInfos)))
    }
  }

  def viewManagedProperties(agentCode: Long) = authenticated { implicit request => {
    propertyLinks.linkedProperties(request.organisationId, 1, 100, requestTotalRowCount = false) map { response =>
      val filteredProps = response.propertyLinks.filter(_.agents.map(_.agentCode).contains(agentCode))
      if (filteredProps.nonEmpty) {
        val organisationName = filteredProps.flatMap(_.agents).filter(_.agentCode == agentCode).head.organisationName
        Ok(views.html.dashboard.managedByAgentsProperties(ManagedPropertiesVM(organisationName, agentCode, filteredProps)))
      }
      else
        NotFound(Global.notFoundTemplate)
    }
  }
  }

  def clientProperties(organisationId: Long) = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      propertyLinks.clientProperties(organisationId, request.organisationId) map { props =>
        if (props.exists(_.authorisedPartyStatus == RepresentationApproved)) {
          val filteredProps: Seq[ClientProperty] = props.filter(_.authorisedPartyStatus == RepresentationApproved)
          Ok(views.html.dashboard.clientProperties(ClientPropertiesVM(filteredProps)))
        } else NotFound
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def draftCases() = authenticated { implicit request =>
    if (ApplicationConfig.casesEnabled) {
      val dummyData = Seq(
        DraftCase(1234, 146440182, "4, EX2 7LL", 123456789, new LocalDate(2017, 1, 3), "Agent ltd", "Check", new LocalDate(2017, 2, 3)),
        DraftCase(2345, 146440182, "1, RG2 9WX", 321654987, new LocalDate(2017, 1, 6), "Agent ltd", "Check", new LocalDate(2017, 2, 6))
      )
      Future.successful(Ok(views.html.dashboard.draftCases(DraftCasesVM(dummyData))))
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }
}

object Dashboard extends Dashboard

case class ManagePropertiesVM(organisationId: Long, properties: Seq[PropertyLink], pagination: Pagination)
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

case class Pagination(pageNumber: Int, pageSize: Int, totalResults: Int) {
  val startPoint = Pagination.getStartPoint(pageNumber, pageSize)
}

object Pagination {
  def getStartPoint(pageNumber: Int, pageSize: Int): Int = pageSize * (pageNumber - 1) + 1
}