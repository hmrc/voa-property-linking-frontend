/*
 * Copyright 2022 HM Revenue & Customs
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
import com.google.inject.{Inject, Singleton}
import config.ApplicationConfig
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import controllers.{PaginationSearchSort, PropertyLinkingController, ValidPagination}
import form.FormValidation._
import models._
import models.searchApi.AgentAuthResult
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class RepresentationController @Inject()(
      val errorHandler: CustomErrorHandler,
      reprConnector: PropertyRepresentationConnector,
      vmvConnector: VmvConnector,
      authenticated: AuthenticatedAction,
      propertyLinkConnector: PropertyLinkConnector,
      revokeClientPropertyView: views.html.propertyrepresentation.revokeClient,
      confirmRevokeClientPropertyView: views.html.propertyrepresentation.confirmRevokeClientProperty,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      val config: ApplicationConfig
) extends PropertyLinkingController with ValidPagination {

  def viewClientProperties(): Action[AnyContent] = authenticated.asAgent { _ =>
    Future.successful(Redirect(config.dashboardUrl("client-properties")))
  }

  def revokeClient(plSubmissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkConnector.clientPropertyLink(plSubmissionId) map {
      case Some(property) => Ok(revokeClientPropertyView(property))
      case None           => notFound
    }
  }

  def revokeClientPropertyConfirmed(uarn: Long, plSubmissionId: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      for {
        property <- vmvConnector.getPropertyHistory(uarn)
        _        <- reprConnector.revokeClientProperty(plSubmissionId)
      } yield Ok(confirmRevokeClientPropertyView(property.addressFull))
  }

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
