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
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.mvc.Action

import scala.concurrent.Future

trait Dashboard extends PropertyLinkingController {
  val propertyLinks = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val auth = Wiring().authConnector
  val authenticated = Wiring().authenticated

  def home() = authenticated.withAccounts { implicit request =>
    Ok(views.html.dashboard.home(request.individualAccount.details, request.organisationAccount))
  }

  def manageProperties() = authenticated { implicit request =>
    propertyLinks.linkedProperties(request.organisationId) map { props =>
      Ok(views.html.dashboard.manageProperties(ManagePropertiesVM(props)))
    }
  }

  def manageAgents() = authenticated { implicit request =>
    propertyLinks.linkedProperties(request.organisationId) map { props =>
      val agentInfos = props.flatMap(_.agents.map(a=> AgentInfo(a.organisationName, a.agentCode)))
      Ok(views.html.dashboard.manageAgents(ManageAgentsVM(agentInfos)))
    }
  }

  def assessments(authorisationId: Long, linkPending: Boolean) = authenticated { implicit request =>
    val backLink = request.headers.get("Referer")
    propertyLinks.assessments(authorisationId) map { assessments =>
      Ok(views.html.dashboard.assessments(
        AssessmentsVM(
          assessments,
          backLink,
          linkPending
        )))
    }
  }

  def viewSummary(uarn: Long) = Action { implicit request =>
    Redirect(ApplicationConfig.vmvUrl + s"/detail/2017/$uarn")
  }

  def viewDetailedAssessment(authorisationId: Long, assessmentRef: Long) = authenticated { implicit request =>
    Redirect(ApplicationConfig.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef"))
  }

  def draftCases() = authenticated { implicit request =>
    if (ApplicationConfig.readyForPrimeTime) {
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

case class ManagePropertiesVM(properties: Seq[PropertyLink])

case class ManageAgentsVM(agents: Seq[AgentInfo])

case class AssessmentsVM(assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean)

case class DraftCasesVM(draftCases: Seq[DraftCase])

case class PropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType, linkedDate: DateTime,
                                       representations: Seq[PropertyRepresentation])

case class PendingPropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType,
                                              linkedDate: DateTime, representations: Seq[PropertyRepresentation])

case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])

case class AgentInfo(organisationName: String, agentCode: Long)
