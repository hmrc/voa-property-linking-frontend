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

import cats.data.OptionT
import cats.instances.future._
import config.{ApplicationConfig, Global, Wiring}
import controllers.{PropertyLinkingController, ValidPagination}
import controllers.agent.RepresentationController.ManagePropertiesVM
import models._

trait RepresentationController extends PropertyLinkingController with ValidPagination {
  val reprConnector = Wiring().propertyRepresentationConnector
  val authenticated = Wiring().authenticated
  val propertyLinkConnector = Wiring().propertyLinkConnector

  def manageRepresentationRequest(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize) { pagination =>
      reprConnector.forAgent(RepresentationApproved, request.organisationId, pagination).map { reprs =>
        Ok(views.html.dashboard.manageClients(ManagePropertiesVM(reprs, request.agentCode)))
      }
    }
  }

  def pendingRepresentationRequest() = authenticated.asAgent { implicit request =>
    withValidPagination(1, 15) { pagination =>
      reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
        Ok(views.html.dashboard.pendingPropertyRepresentations(ManagePropertiesVM(reprs, request.agentCode)))
      }
    }
  }

  def accept(submissionId: String, noOfPendingRequests: Long) = authenticated.asAgent { implicit request =>
    val response = RepresentationResponse(submissionId, request.personId.toLong, RepresentationResponseApproved)
    reprConnector.response(response).map { _ =>
      val continueLink = if (noOfPendingRequests > 1) {
        controllers.agent.routes.RepresentationController.pendingRepresentationRequest().url
      } else {
        controllers.agent.routes.RepresentationController.manageRepresentationRequest().url
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
        controllers.agent.routes.RepresentationController.manageRepresentationRequest().url
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
      Redirect(controllers.routes.Dashboard.clientProperties(clientOrganisationId))
    }).getOrElse(notFound)
  }
}

object RepresentationController extends RepresentationController {
  case class ManagePropertiesVM(propertyRepresentations: PropertyRepresentations, agentCode: Long)
}
