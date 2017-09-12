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

package controllers.agent

import actions.AuthenticatedAction
import cats.data.OptionT
import cats.instances.future._
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import connectors.PropertyRepresentationConnector
import controllers.{Pagination, PropertyLinkingController, ValidPagination}
import models._
import play.api.libs.json.Json
import controllers.agent.RepresentationController.ManagePropertiesVM
import models.searchApi.AgentAuthResult

@Singleton()
class RepresentationController @Inject()(config: ApplicationConfig,
                                         reprConnector: PropertyRepresentationConnector,
                                         authenticated: AuthenticatedAction,
                                         propertyLinkConnector: PropertyLinkConnector)
  extends PropertyLinkingController with ValidPagination {

  def viewClientProperties(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize) { pagination =>
      if (config.searchSortEnabled) {

        for{
          totalPendingRequests <- reprConnector.forAgentSearchAndSort(agentOrganisationId = request.organisationId,
                                                                      pagination =  pagination,
                                                                      status = Some(RepresentationPending.toString))
          clientResponse       <- reprConnector.forAgentSearchAndSort(request.organisationId, pagination)

        }yield {
            Ok(views.html.dashboard.manageClientsSearchSort(
              ManageClientPropertiesSearchAndSortVM(
                result = clientResponse,
                totalPendingRequests = totalPendingRequests.filterTotal,
                pagination = pagination.copy(totalResults = clientResponse.total)))
            )
        }

      } else {
        reprConnector.forAgent(RepresentationApproved, request.organisationId, pagination).map { reprs =>
          Ok(views.html.dashboard.manageClients(ManagePropertiesVM(reprs.propertyRepresentations,
            reprs.totalPendingRequests,
            pagination.copy(totalResults = reprs.resultCount.getOrElse(0L)))))
        }
      }
    }
  }

  def listRepresentationRequest(page: Int, pageSize: Int, requestTotalRowCount: Boolean) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
      reprConnector.forAgent(RepresentationApproved, request.organisationId, pagination).map { reprs =>
        Ok(Json.toJson(reprs))
      }
    }
  }


  def listRepresentationRequestSearchAndSort(page: Int,
                                 pageSize: Int,
                                 requestTotalRowCount: Boolean,
                                 sortfield: Option[String],
                                 sortorder: Option[String],
                                 status: Option[String],
                                 address: Option[String],
                                 baref: Option[String],
                                             client: Option[String])  = authenticated { implicit request =>
    withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
      reprConnector.forAgentSearchAndSort(request.organisationId, pagination, sortfield, sortorder, status, address, baref, client) map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  def pendingRepresentationsJson(page: Int, pageSize: Int, requestTotalRowCount: Boolean) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
      reprConnector.forAgent(RepresentationPending, request.organisationId, pagination) map { r =>
        Ok(Json.toJson(r))
      }
    }
  }

  def pendingRepresentationRequest(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize) { pagination =>
      reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
        Ok(views.html.dashboard.pendingPropertyRepresentations(
          ManagePropertiesVM(
            reprs.propertyRepresentations,
            reprs.totalPendingRequests,
            pagination.copy(totalResults = reprs.resultCount.getOrElse(0L))
          )
        ))
      }
    }
  }

  def accept(submissionId: String, noOfPendingRequests: Long) = authenticated.asAgent { implicit request =>
    val response = RepresentationResponse(submissionId, request.personId.toLong, RepresentationResponseApproved)
    reprConnector.response(response).map { _ =>
      val continueLink = if (noOfPendingRequests > 1) {
        controllers.agent.routes.RepresentationController.pendingRepresentationRequest().url
      } else {
        controllers.agent.routes.RepresentationController.viewClientProperties().url
      }
      Ok(views.html.propertyRepresentation.requestAccepted(continueLink))
    }
  }

  def reject(submissionId: String, noOfPendingRequests: Long) = authenticated.asAgent { implicit request =>
    val response = RepresentationResponse(submissionId, request.personId.toLong, RepresentationResponseDeclined)
    reprConnector.response(response).map { _ =>
      val continueLink = if (noOfPendingRequests > 1) {
        controllers.agent.routes.RepresentationController.pendingRepresentationRequest().url
      } else {
        controllers.agent.routes.RepresentationController.viewClientProperties().url
      }
      Ok(views.html.propertyRepresentation.requestRejected(continueLink))
    }
  }

  def revokeClient(authorisationId: Long, clientOrganisationId: Long) = authenticated.asAgent { implicit request =>
    propertyLinkConnector.clientProperty(authorisationId, clientOrganisationId, request.organisationAccount.id) map {
      case Some(property) => Ok(views.html.propertyRepresentation.revokeClient(property))
      case None => notFound
    }
  }

  def revokeClientConfirmed(authorisationId: Long, clientOrganisationId: Long) = authenticated.asAgent { implicit request =>
    (for {
      clientProperty <- OptionT(propertyLinkConnector.clientProperty(authorisationId, clientOrganisationId, request.organisationAccount.id))
      _ <- OptionT.liftF(reprConnector.revoke(clientProperty.authorisedPartyId))
    } yield {
      Redirect(routes.RepresentationController.viewClientProperties())
    }).getOrElse(notFound)
  }
}

object RepresentationController {
  case class ManagePropertiesVM(propertyRepresentations: Seq[PropertyRepresentation], totalPendingRequests: Long, pagination: Pagination)
}

case class ManageClientPropertiesSearchAndSortVM(result: AgentAuthResult, totalPendingRequests: Long, pagination: Pagination)
