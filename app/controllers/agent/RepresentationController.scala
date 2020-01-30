/*
 * Copyright 2020 HM Revenue & Customs
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
import actions.requests.AgentRequest
import cats.data.OptionT
import cats.instances.future._
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.agent.RepresentationController.ManagePropertiesVM
import controllers.{Pagination, PaginationSearchSort, PropertyLinkingController, ValidPagination}
import form.FormValidation._
import models._
import models.searchApi.AgentAuthResult
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton()
class RepresentationController @Inject()(
      val errorHandler: CustomErrorHandler,
      reprConnector: PropertyRepresentationConnector,
      authenticated: AuthenticatedAction,
      propertyLinkConnector: PropertyLinkConnector,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      val config: ApplicationConfig
) extends PropertyLinkingController with ValidPagination {

  def viewClientProperties(): Action[AnyContent] = authenticated.asAgent { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("client-properties")))
  }

  def getMyClientsPropertyLinkRequests(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    withValidPagination(page, pageSize) { pagination =>
      reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
        if (reprs.totalPendingRequests > 0 && reprs.propertyRepresentations.isEmpty) {
          reprConnector
            .forAgent(
              RepresentationPending,
              request.organisationId,
              pagination.copy(pageNumber = pagination.pageNumber - 1))
            .map { reprs =>
              Ok(views.html.dashboard.pendingPropertyRepresentations(
                BulkActionsForm.form,
                ManagePropertiesVM(
                  reprs.propertyRepresentations,
                  reprs.totalPendingRequests,
                  pagination.copy(totalResults = reprs.totalPendingRequests)
                )
              ))
            }
        } else {
          Future.successful(Ok(views.html.dashboard.pendingPropertyRepresentations(
            BulkActionsForm.form,
            ManagePropertiesVM(
              reprs.propertyRepresentations,
              reprs.totalPendingRequests,
              pagination.copy(totalResults = reprs.totalPendingRequests)
            )
          )))
        }
      }
    }
  }

  def submitMyClientsPropertyLinkRequestResponse(page: Int, pageSize: Int) = authenticated.asAgent {
    implicit request =>
      BulkActionsForm.form
        .bindFromRequest()
        .fold(
          errors => {
            withValidPagination(page, pageSize) {
              pagination =>
                reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
                  BadRequest(views.html.dashboard.pendingPropertyRepresentations(
                    errors,
                    ManagePropertiesVM(
                      propertyRepresentations = reprs.propertyRepresentations,
                      totalPendingRequests = reprs.totalPendingRequests,
                      pagination = pagination.copy(totalResults = reprs.totalPendingRequests)
                    )
                  ))
                }
            }
          },
          data => {
            data.action match {
              case "reject" =>
                Future.successful(
                  Ok(views.html.dashboard.pendingPropertyRepresentationsConfirm(data, BulkActionsForm.form)))
              case _ =>
                withValidPagination(page, pageSize) { pagination =>
                  reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).flatMap { reprs =>
                    val futureListOfSuccesses =
                      getFutureListOfActions(data, request.personId).map(_.filter(_.isSuccess))
                    futureListOfSuccesses.map(successes =>
                      routePendingRequests(successes.size, data.copy(action = "accept-confirm"), pagination, reprs)(
                        request))
                  }
                }
            }
          }
        )
  }

  def cancel(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    BulkActionsForm.form
      .bindFromRequest()
      .fold(
        errors => {
          withValidPagination(page, pageSize) {
            pagination =>
              reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
                BadRequest(views.html.dashboard.pendingPropertyRepresentations(
                  errors,
                  ManagePropertiesVM(
                    propertyRepresentations = reprs.propertyRepresentations,
                    totalPendingRequests = reprs.totalPendingRequests,
                    pagination = pagination.copy(totalResults = reprs.totalPendingRequests)
                  )
                ))
              }
          }
        },
        data => {
          withValidPagination(page, pageSize) { pagination =>
            reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
              routePendingRequests(
                completedActions = 0,
                data = data,
                pagination = pagination,
                reprs = reprs,
                afterCancel = true)
            }
          }
        }
      )
  }

  def continue(page: Int, pageSize: Int) = authenticated.asAgent { implicit request =>
    BulkActionsForm.form
      .bindFromRequest()
      .fold(
        _ => Future.successful(BadRequest(errorHandler.badRequestTemplate)),
        data => {
          withValidPagination(page, pageSize) { pagination =>
            reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
              routePendingRequests(
                completedActions = data.complete.getOrElse(0),
                data = data,
                pagination = pagination,
                reprs = reprs,
                afterCancel = false)
            }
          }
        }
      )
  }

  def bulkActions(): Action[AnyContent] = authenticated.asAgent { implicit request =>
    BulkActionsForm.form
      .bindFromRequest()
      .fold(
        _ => Future.successful(BadRequest(errorHandler.badRequestTemplate)),
        data => {
          val futureListOfSuccesses = getFutureListOfActions(data, request.personId).map(_.filter(_.isSuccess))
          futureListOfSuccesses.flatMap(successes =>
            withValidPagination(data.page, data.pageSize) { pagination =>
              reprConnector.forAgent(RepresentationPending, request.organisationId, pagination).map { reprs =>
                routePendingRequests(successes.size, data, pagination, reprs)(request)
              }
          })
        }
      )
  }

  private def routePendingRequests(
        completedActions: Int,
        data: RepresentationBulkAction,
        pagination: Pagination,
        reprs: PropertyRepresentations,
        afterCancel: Boolean = false)(implicit request: AgentRequest[_]): Result = {
    def getModel =
      ManagePropertiesVM(
        propertyRepresentations = reprs.propertyRepresentations,
        totalPendingRequests = reprs.totalPendingRequests,
        pagination = pagination.copy(
          pageNumber =
            if (reprs.propertyRepresentations.isEmpty) Math.max(1, pagination.pageNumber - 1)
            else pagination.pageNumber,
          totalResults = reprs.totalPendingRequests
        ),
        action = Some(data.action.toLowerCase),
        requestIds = Some(data.requestIds),
        complete = Some(completedActions),
        afterCancel = afterCancel
      )

    if (data.action.toLowerCase == "accept-confirm") {
      Ok(views.html.propertyrepresentation.requestAccepted(BulkActionsForm.form, getModel))
    } else if (data.action.toLowerCase == "reject-confirm") {
      Ok(views.html.propertyrepresentation.requestRejected(BulkActionsForm.form, getModel))
    } else if (reprs.totalPendingRequests > 0 && reprs.propertyRepresentations.nonEmpty) {
      Ok(views.html.dashboard.pendingPropertyRepresentations(BulkActionsForm.form, getModel))
    } else if (reprs.totalPendingRequests > 0) {
      Redirect(
        routes.RepresentationController
          .getMyClientsPropertyLinkRequests(Math.max(1, pagination.pageNumber - 1), pagination.pageSize))
    } else {
      Redirect(routes.RepresentationController.viewClientProperties())
    }
  }

  private def getFutureListOfActions(data: RepresentationBulkAction, personId: Long)(
        implicit hc: HeaderCarrier): Future[List[Try[Unit]]] = {
    def futureToFutureTry(f: Future[Unit]): Future[Try[Unit]] = f.map(Try.apply(_)).recover({ case x => Failure(x) })

    val actionType =
      if (data.action.toLowerCase == "accept") RepresentationResponseApproved else RepresentationResponseDeclined

    Future.sequence(
      data.requestIds
        .map(id => reprConnector.response(RepresentationResponse(id, personId, actionType)))
        .map(futureToFutureTry))
  }

  def revokeClient(authorisationId: Long, clientOrganisationId: Long): Action[AnyContent] = authenticated.asAgent {
    implicit request =>
      propertyLinkConnector.clientProperty(authorisationId, clientOrganisationId, request.organisationAccount.id) map {
        case Some(property) => Ok(views.html.propertyrepresentation.revokeClient(property))
        case None           => notFound
      }
  }

  def revokeClientConfirmed(authorisationId: Long, clientOrganisationId: Long) = authenticated.asAgent {
    implicit request =>
      (for {
        clientProperty <- OptionT(
                           propertyLinkConnector
                             .clientProperty(authorisationId, clientOrganisationId, request.organisationAccount.id))
        _ <- OptionT.liftF(reprConnector.revoke(clientProperty.authorisedPartyId))
      } yield {
        Redirect(config.newDashboardUrl("client-properties"))
      }).getOrElse(notFound)
  }

}

object RepresentationController {

  case class ManagePropertiesVM(
        propertyRepresentations: Seq[PropertyRepresentation],
        totalPendingRequests: Long,
        pagination: Pagination,
        action: Option[String] = None,
        requestIds: Option[List[String]] = None,
        complete: Option[Int] = None,
        afterCancel: Boolean = false)

}

object BulkActionsForm {
  lazy val form: Form[RepresentationBulkAction] = Form(
    mapping(
      "page"       -> number,
      "pageSize"   -> number,
      "action"     -> text,
      "requestIds" -> list(text).verifying(nonEmptyList),
      "complete"   -> optional(number)
    )(RepresentationBulkAction.apply)(RepresentationBulkAction.unapply))
}

case class ManageClientPropertiesVM(
      result: AgentAuthResult,
      totalPendingRequests: Long,
      pagination: PaginationSearchSort)
