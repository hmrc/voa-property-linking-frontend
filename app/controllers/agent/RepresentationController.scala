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

package controllers.agent

import actions.{AgentRequest, AuthenticatedAction}
import binders.pagination.PaginationParameters
import binders.searchandsort.SearchAndSort
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import connectors.{MessagesConnector, PropertyRepresentationConnector}
import controllers.PropertyLinkingController
import controllers.agent.RepresentationController.ManagePropertiesVM
import exceptionhandler.ErrorHandler
import form.FormValidation._
import models._
import models.searchApi.AgentAuthResult
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton()
class RepresentationController @Inject()(reprConnector: PropertyRepresentationConnector,
                                         authenticated: AuthenticatedAction,
                                         propertyLinkConnector: PropertyLinkConnector,
                                         messagesConnector: MessagesConnector,
                                         errorHandler: ErrorHandler
                                        )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController {

  def viewClientProperties(): Action[AnyContent] = authenticated.asAgent { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("client-properties")))
  }


  def listRepresentationRequest(
                               pagination: PaginationParameters,
                               searchAndSort: SearchAndSort
                               ): Action[AnyContent] = authenticated { implicit request =>
      reprConnector.forAgentSearchAndSort(request.organisationId, pagination, searchAndSort) map { res =>
        Ok(Json.toJson(res))
      }
  }

  def pendingRepresentationRequest(pagination: PaginationParameters): Action[AnyContent] = authenticated.asAgent { implicit request =>
      reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
        if (reprs.totalPendingRequests > 0 && reprs.propertyRepresentations.size == 0) {
          reprConnector.forAgent(RepresentationPending,
            request.organisationId,
            pagination.copy(page = pagination.page - 1)).map { reprs =>

            Ok(views.html.dashboard.pendingPropertyRepresentations(
              BulkActionsForm.form,
              ManagePropertiesVM(
                reprs.propertyRepresentations,
                reprs.totalPendingRequests,
                pagination
              )
            ))
          }
        } else {
          Future.successful(Ok(views.html.dashboard.pendingPropertyRepresentations(
            BulkActionsForm.form,
            ManagePropertiesVM(
              reprs.propertyRepresentations,
              reprs.totalPendingRequests,
              pagination
            )
          )))
        }
      }
  }

  def confirm(pagination: PaginationParameters): Action[AnyContent] = authenticated.asAgent { implicit request =>
    BulkActionsForm.form.bindFromRequest().fold(
      errors => {
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
            Future.successful(BadRequest(views.html.dashboard.pendingPropertyRepresentations(errors,
              ManagePropertiesVM(
                propertyRepresentations = reprs.propertyRepresentations,
                totalPendingRequests = reprs.totalPendingRequests,
                pagination = pagination
              ))))
        }
      },
      data => {
        if (data.action == "reject") {
          Future.successful(Ok(views.html.dashboard.pendingPropertyRepresentationsConfirm(data, BulkActionsForm.form)))
        } else {
            reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
              val futureListOfSuccesses = getFutureListOfActions(data, request.personId).map(_.filter(_.isSuccess))
              futureListOfSuccesses.map(successes =>
                routePendingRequests(successes.size, data.copy(action = "accept-confirm"), pagination, reprs)(request))
            }
        }
      })
  }

  def cancel(pagination: PaginationParameters) = authenticated.asAgent { implicit request =>
    BulkActionsForm.form.bindFromRequest().fold(
      errors => {
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
            Future.successful(BadRequest(views.html.dashboard.pendingPropertyRepresentations(errors,
              ManagePropertiesVM(
                propertyRepresentations = reprs.propertyRepresentations,
                totalPendingRequests = reprs.totalPendingRequests,
                pagination = pagination
              ))))
          }
      },
      data => {
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
            routePendingRequests(
              completedActions = 0,
              data = data,
              pagination = pagination,
              reprs = reprs,
              afterCancel = true)
          }
      })
  }

  def continue(pagination: PaginationParameters) = authenticated.asAgent { implicit request =>
    BulkActionsForm.form.bindFromRequest().fold(
      _ => Future.successful(errorHandler.badRequest),
      data => {
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
            routePendingRequests(
              completedActions = data.complete.getOrElse(0),
              data = data,
              pagination = pagination,
              reprs = reprs,
              afterCancel = false)
          }
      })
  }

  def bulkActions(): Action[AnyContent] = authenticated.asAgent { implicit request =>
    BulkActionsForm.form.bindFromRequest().fold(
      _ => Future.successful(errorHandler.badRequest),
      data => {
        val futureListOfSuccesses = getFutureListOfActions(data, request.personId).map(_.filter(_.isSuccess))
        futureListOfSuccesses.flatMap{ successes =>
          val pagination = PaginationParameters(data.page, data.pageSize)
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
              routePendingRequests(successes.size, data, pagination, reprs)(request)
            }
        }
      })
  }

  private def routePendingRequests(completedActions: Int,
                                   data: RepresentationBulkAction,
                                   pagination: PaginationParameters,
                                   reprs: PropertyRepresentations,
                                   afterCancel: Boolean = false)(implicit request: AgentRequest[_]): Result = {
    def getModel = {
      ManagePropertiesVM(
        propertyRepresentations = reprs.propertyRepresentations,
        totalPendingRequests = reprs.totalPendingRequests,
        pagination = pagination.copy(
          page = if (reprs.propertyRepresentations.size == 0) Math.max(1, pagination.page - 1)
          else pagination.page),
        action = Some(data.action.toLowerCase),
        requestIds = Some(data.requestIds),
        complete = Some(completedActions),
        afterCancel = afterCancel)
    }

    if (data.action.toLowerCase == "accept-confirm") {
      Ok(views.html.propertyRepresentation.requestAccepted(
        BulkActionsForm.form,
        getModel))
    } else if (data.action.toLowerCase == "reject-confirm") {
      Ok(views.html.propertyRepresentation.requestRejected(
        BulkActionsForm.form,
        getModel))
    } else if (reprs.totalPendingRequests > 0 && reprs.propertyRepresentations.size > 0) {
      Ok(views.html.dashboard.pendingPropertyRepresentations(
        BulkActionsForm.form,
        getModel))
    } else if (reprs.totalPendingRequests > 0) {
      Redirect(routes.RepresentationController.pendingRepresentationRequest(pagination.copy(page = Math.max(1, pagination.page - 1))))
    } else {
      Redirect(routes.RepresentationController.viewClientProperties())
    }
  }

  private def getFutureListOfActions(data: RepresentationBulkAction, personId: Long)(implicit hc: HeaderCarrier) = {
    def futureToFutureTry(f: Future[Unit]): Future[Try[Unit]] = f.map(Success(_)).recover({ case x => Failure(x) })

    val actionType =
      if (data.action.toLowerCase == "accept") RepresentationResponseApproved else RepresentationResponseDeclined

    Future.sequence(data.requestIds.map(id =>
      reprConnector.response(RepresentationResponse(id, personId, actionType))).map(futureToFutureTry))
  }

  def revokeClient(authorisationId: Long, clientOrganisationId: Long) = authenticated.asAgent { implicit request =>
    propertyLinkConnector.clientProperty(authorisationId, clientOrganisationId, request.organisationAccount.id) map { property =>
      Ok(views.html.propertyRepresentation.revokeClient(property))
    }
  }

  def revokeClientConfirmed(authorisationId: Long, clientOrganisationId: Long) = authenticated.asAgent { implicit request =>
    for {
      clientProperty <- propertyLinkConnector.clientProperty(authorisationId, clientOrganisationId, request.organisationAccount.id)
      _ <- reprConnector.revoke(clientProperty.authorisedPartyId)
    } yield Redirect(config.newDashboardUrl("client-properties"))
  }

}

object RepresentationController {

  case class ManagePropertiesVM(propertyRepresentations: Seq[PropertyRepresentation],
                                totalPendingRequests: Long,
                                pagination: PaginationParameters,
                                action: Option[String] = None,
                                requestIds: Option[List[String]] = None,
                                complete: Option[Int] = None,
                                afterCancel: Boolean = false
                               )

}


object BulkActionsForm {
  lazy val form: Form[RepresentationBulkAction] = Form(mapping(
    "page" -> number(1),
    "pageSize" -> number(10, 100),
    "action" -> text,
    "requestIds" -> list(text).verifying(nonEmptyList),
    "complete" -> optional(number)
  )(RepresentationBulkAction.apply)(RepresentationBulkAction.unapply))
}

case class ManageClientPropertiesVM(
                                     result: AgentAuthResult,
                                     totalPendingRequests: Long,
                                     pagination: PaginationParameters,
                                     searchAndSort: SearchAndSort
                                   )

