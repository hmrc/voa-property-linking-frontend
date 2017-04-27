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
import controllers.PropertyLinkingController
import controllers.agent.RepresentationController.ManagePropertiesVM
import models._

import scala.concurrent.Future

trait RepresentationController extends PropertyLinkingController {
  val reprConnector = Wiring().propertyRepresentationConnector
  val authenticated = Wiring().authenticated
  val propertyLinkConnector = Wiring().propertyLinkConnector

  def manageRepresentationRequest() = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      reprConnector.forAgent(RepresentationApproved, request.organisationId).map { reprs =>
        Ok(views.html.dashboard.manageClients(ManagePropertiesVM(reprs, request.agentCode)))
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }


  def pendingRepresentationRequest() = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      reprConnector.forAgent(RepresentationPending, request.organisationId).map { reprs =>
        Ok(views.html.dashboard.pendingPropertyRepresentations(ManagePropertiesVM(reprs, request.agentCode)))
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def accept(submissionId: String, noOfPendingRequests: Long) = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      val response = RepresentationResponse(submissionId, request.personId.toLong, RepresentationResponseApproved)
      reprConnector.response(response).map { _ =>
        val continueLink = if (noOfPendingRequests > 1) {
          controllers.agent.routes.RepresentationController.pendingRepresentationRequest().url
        } else {
          controllers.agent.routes.RepresentationController.manageRepresentationRequest().url
        }
        Ok(views.html.propertyRepresentation.requestAccepted(continueLink))
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def reject(submissionId: String, noOfPendingRequests: Long) = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      val response = RepresentationResponse(submissionId, request.personId.toLong, RepresentationResponseDeclined)
      reprConnector.response(response).map { _ =>
        val continueLink = if (noOfPendingRequests > 1) {
          controllers.agent.routes.RepresentationController.pendingRepresentationRequest().url
        } else {
          controllers.agent.routes.RepresentationController.manageRepresentationRequest().url
        }
        Ok(views.html.propertyRepresentation.requestRejected(continueLink))
      }
    } else {
      NotFound(Global.notFoundTemplate)
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
