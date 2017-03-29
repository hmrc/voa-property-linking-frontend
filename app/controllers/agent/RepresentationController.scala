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
      reprConnector.forAgent(RepresentationApproved.name, request.organisationId).map { reprs =>
        Ok(views.html.dashboard.manageClients(ManagePropertiesVM(reprs, request.agentCode)))
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }


  def pendingRepresentationRequest() = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      reprConnector.forAgent(RepresentationPending.name, request.organisationId).map { reprs =>
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

  def revokeClient(organisationId: Long, authorisedPartyId: Long) = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      for {
        clientProperties <- propertyLinkConnector.clientProperties(organisationId, request.organisationAccount.id)
        clientProperty = clientProperties.filter(_.authorisedPartyId == authorisedPartyId)
      } yield {
        if (clientProperty.nonEmpty)
          Ok(views.html.propertyRepresentation.revokeClient(clientProperty.head))
        else
          NotFound(Global.notFoundTemplate)
      }
    } else {
      NotFound(Global.notFoundTemplate)
    }
  }

  def revokeClientConfirmed(organisationId: Long, authorisedPartyId: Long) = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      (for {
        clientProperties <- OptionT.liftF(propertyLinkConnector.clientProperties(organisationId, request.organisationAccount.id))
        prop <- OptionT.fromOption(clientProperties.find(_.authorisedPartyId == authorisedPartyId))
        _ <- OptionT.liftF(reprConnector.revoke(prop.authorisedPartyId))
      } yield {
        if (clientProperties.size > 1)
          Redirect(controllers.routes.Dashboard.clientProperties(organisationId))
        else
          Redirect(controllers.agent.routes.RepresentationController.manageRepresentationRequest())
      }).value.map(_.getOrElse(NotFound(Global.notFoundTemplate)))
    } else {
      Future.successful(NotFound(Global.notFoundTemplate))
    }
  }


}
object RepresentationController extends RepresentationController {

  case class ManagePropertiesVM(propertyRepresentations: PropertyRepresentations, agentCode: Long)

}
