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

import config.Wiring
import config.ApplicationConfig
import controllers.PropertyLinkingController
import config.Global
import models._

object RepresentationController extends PropertyLinkingController {
  val reprConnector = Wiring().propertyRepresentationConnector
  val authenticated = Wiring().authenticated

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

  def accept(submisstionId: String, noOfPendingRequests: Long) = authenticated.asAgent { implicit request =>
    if (ApplicationConfig.agentEnabled) {
      val response = RepresentationResponse(submisstionId, request.personId.toLong, RepresentationResponseApproved)
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



  case class ManagePropertiesVM(propertyRepresentations: PropertyRepresentations, agentCode: Long)

}
