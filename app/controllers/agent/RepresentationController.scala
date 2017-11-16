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

import actions.{AgentRequest, AuthenticatedAction}
import cats.data.OptionT
import cats.instances.future._
import com.google.inject.{Inject, Singleton}
import config.{ApplicationConfig, Global}
import connectors.propertyLinking.PropertyLinkConnector
import connectors.{MessagesConnector, PropertyRepresentationConnector}
import controllers.agent.RepresentationController.ManagePropertiesVM
import controllers.{Pagination, PaginationSearchSort, PropertyLinkingController, ValidPagination}
import models._
import models.searchApi.AgentAuthResult
import play.api.data.Forms.mapping
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import play.mvc.Http.Request
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton()
class RepresentationController @Inject()(config: ApplicationConfig,
                                         reprConnector: PropertyRepresentationConnector,
                                         authenticated: AuthenticatedAction,
                                         propertyLinkConnector: PropertyLinkConnector,
                                         messagesConnector: MessagesConnector)
  extends PropertyLinkingController with ValidPagination {

  def viewClientProperties(page: Int, pageSize: Int, requestTotalRowCount: Boolean = true) =
    viewClientPropertiesSearchSort(page = page, pageSize = pageSize, requestTotalRowCount = requestTotalRowCount, None, None, None, None, None, None)


  def viewClientPropertiesSearchSort(page: Int, pageSize: Int, requestTotalRowCount: Boolean = true, sortfield: Option[String] = None,
                                     sortorder: Option[String] = None, status: Option[String] = None, address: Option[String] = None,
                                     baref: Option[String] = None, client: Option[String] = None) = authenticated.asAgent { implicit request =>
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
        client = client
      ) { paginationSearchSort =>
        for {
          reprs <- reprConnector.forAgentSearchAndSort(request.organisationId, paginationSearchSort)
          msgCount <- messagesConnector.countUnread(request.organisationId)
        } yield {
          Ok(views.html.dashboard.manageClientsSearchSort(
            ManageClientPropertiesSearchAndSortVM(
              result = reprs,
              totalPendingRequests = reprs.pendingRepresentations,
              pagination = paginationSearchSort.copy(totalResults = reprs.filterTotal)
            ),
            msgCount.unread
          ))
        }
      }
    } else {
      withValidPagination(page, pageSize, requestTotalRowCount) { pagination =>
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
                                             client: Option[String]) = authenticated { implicit request =>
    withValidPaginationSearchSort(
      page = page,
      pageSize = pageSize,
      requestTotalRowCount = requestTotalRowCount,
      sortfield = sortfield,
      sortorder = sortorder,
      status = status,
      address = address,
      baref = baref,
      client = client
    ) { pagination =>
          reprConnector.forAgentSearchAndSort(request.organisationId, pagination) map { res =>
           Ok(Json.toJson(res))
      }
    }
  }

  def pendingRepresentationRequest(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize) { pagination =>
      reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
        Ok(views.html.dashboard.pendingPropertyRepresentations(
          BulkActionsForm.form,
          ManagePropertiesVM(
            reprs.propertyRepresentations,
            reprs.totalPendingRequests,
            pagination.copy(totalResults = reprs.resultCount.getOrElse(0L))
          )
        ))
      }
    }
  }

  def confirm(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    BulkActionsForm.form.bindFromRequest().fold(
      errors => {
        withValidPagination(page, pageSize) { pagination =>
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
            BadRequest(views.html.dashboard.pendingPropertyRepresentations(errors,
              ManagePropertiesVM(
                propertyRepresentations = reprs.propertyRepresentations,
                totalPendingRequests = reprs.totalPendingRequests,
                pagination = pagination.copy(totalResults = reprs.resultCount.getOrElse(0L))
              )))
          }
        }
      },
      data => {
        if (data.action == "reject") {
          Future.successful(Ok(views.html.dashboard.pendingPropertyRepresentationsConfirm(data, BulkActionsForm.form)))
        } else {
          val futureListOfSuccesses = getFutureListOfActions(data, request.personId).map(_.filter(_.isSuccess))
          futureListOfSuccesses.flatMap(successes =>
            withValidPagination(page, pageSize) { pagination =>
              reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
                routePendingRequests(data, successes, pagination, reprs)(request)
              }
            })
        }
      })
  }

  def cancel(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    BulkActionsForm.form.bindFromRequest().fold(
      errors => {
        withValidPagination(page, pageSize) { pagination =>
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
            BadRequest(views.html.dashboard.pendingPropertyRepresentations(errors,
              ManagePropertiesVM(
                propertyRepresentations = reprs.propertyRepresentations,
                totalPendingRequests = reprs.totalPendingRequests,
                pagination = pagination.copy(totalResults = reprs.resultCount.getOrElse(0L))
              )))
          }
        }
      },
      data => {
        withValidPagination(page, pageSize) { pagination =>
          reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
            okPendingPropertyRepresentations(
              numberActions = 0,
              data = data,
              pagination = pagination,
              reprs = reprs,
              afterCancel = true)
          }
        }
      })
  }

  def bulkActions(): Action[AnyContent] = authenticated.asAgent { implicit request =>

    BulkActionsForm.form.bindFromRequest().fold(
      _ => BadRequest(Global.badRequestTemplate),
      data => {
        val futureListOfSuccesses = getFutureListOfActions(data, request.personId).map(_.filter(_.isSuccess))
        futureListOfSuccesses.flatMap(successes =>
          withValidPagination(data.page, data.pageSize) { pagination =>
            reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
              routePendingRequests(data, successes, pagination, reprs)(request)
            }
          })
      })
  }

  private def okPendingPropertyRepresentations(numberActions: Int,
                                   data: RepresentationBulkAction,
                                   pagination: Pagination,
                                   reprs: PropertyRepresentations,
                                   afterCancel:Boolean = false)(implicit request: AgentRequest[_]): Result = {
    Ok(views.html.dashboard.pendingPropertyRepresentations(
      BulkActionsForm.form,
      ManagePropertiesVM(
        propertyRepresentations = reprs.propertyRepresentations,
        totalPendingRequests = reprs.totalPendingRequests,
        pagination = pagination.copy(
          pageNumber = if(reprs.propertyRepresentations.size == 0) Math.max(1, pagination.pageNumber - 1)
                        else pagination.pageNumber,
          totalResults = reprs.resultCount.getOrElse(0L)),
        action = Some(data.action.toLowerCase),
        requestIds = Some(data.requestIds),
        complete = Some(numberActions),
        afterCancel = afterCancel)))
  }

  private def routePendingRequests(data: RepresentationBulkAction,
                                   successes: List[Try[Unit]],
                                   pagination: Pagination,
                                   reprs: PropertyRepresentations)(implicit request: AgentRequest[_]) = {
    if (reprs.totalPendingRequests > 0 && reprs.propertyRepresentations.size > 0) {
      okPendingPropertyRepresentations(successes.size, data, pagination, reprs)
    } else if (reprs.totalPendingRequests > 0) {
      Redirect(routes.RepresentationController.pendingRepresentationRequest(
        Math.max(1, pagination.pageNumber - 1), pagination.pageSize))
    } else {
      Redirect(routes.RepresentationController.viewClientProperties())
    }
  }

  private def getFutureListOfActions[T](data: RepresentationBulkAction, personId: Long)(implicit hc:HeaderCarrier) = {
    def futureToFutureTry[T](f: Future[T]): Future[Try[T]] = f.map(Success(_)).recover({ case x => Failure(x) })

    // do action
    val actionType =
      if (data.action.toLowerCase == "accept") RepresentationResponseApproved else RepresentationResponseDeclined

    val futureListOfTrys = Future.sequence(data.requestIds.map(id =>
      reprConnector.response(RepresentationResponse(id, personId, actionType))).map(futureToFutureTry(_)))

    futureListOfTrys
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

  case class ManagePropertiesVM(propertyRepresentations: Seq[PropertyRepresentation],
                                totalPendingRequests: Long,
                                pagination: Pagination,
                                action: Option[String] = None,
                                requestIds: Option[List[String]] = None,
                                complete: Option[Int] = None,
                                afterCancel: Boolean = false
                               )

}

import form.FormValidation._
import play.api.data.Form
import play.api.data.Forms._

object BulkActionsForm {
  lazy val form: Form[RepresentationBulkAction] = Form(mapping(
    "page" -> number,
    "pageSize" -> number,
    "pending" -> number,
    "action" -> text,
    "requestIds" -> list(text).verifying(nonEmptyList)
  )(RepresentationBulkAction.apply)(RepresentationBulkAction.unapply))
}

case class ManageClientPropertiesSearchAndSortVM(result: AgentAuthResult,
                                                 totalPendingRequests: Long,
                                                 pagination: PaginationSearchSort)

