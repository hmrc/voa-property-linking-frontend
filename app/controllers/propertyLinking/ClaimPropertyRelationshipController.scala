/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.propertyLinking

import java.time.LocalDate

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import actions.requests.AuthenticatedRequest
import binders.propertylinks.GetPropertyLinksParameters
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.SubmissionIdConnector
import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import controllers._
import form.Mappings._
import form.{ConditionalDateAfter, EnumMapping}
import javax.inject.{Inject, Named}
import models._
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepo
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.voa.play.form.ConditionalMappings._
import views.helpers.Errors

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimPropertyRelationshipController @Inject()(
      val errorHandler: CustomErrorHandler,
      val submissionIdConnector: SubmissionIdConnector,
      @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      val propertyLinksConnector: PropertyLinkConnector,
      val vmvConnector: VmvConnector,
      val runModeConfiguration: Configuration,
      relationshipToPropertyView: views.html.propertyLinking.relationshipToProperty)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  import ClaimPropertyRelationship._

  def show(clientDetails: Option[ClientDetails] = None) = authenticatedAction { implicit request =>
    val uri = clientDetails match {
      case Some(client) =>
        s"search?organisationId=${client.organisationId}&organisationName=${client.organisationName}"
      case _ => s"search"
    }
    Redirect(s"${config.vmvUrl}/$uri")
  }

  def checkPropertyLinks() = authenticatedAction.async { implicit request =>
    val pLinks = propertyLinksConnector
      .getMyOrganisationsPropertyLinks(GetPropertyLinksParameters(), PaginationParams(1, 20, false))

    pLinks.map { res =>
      if (res.authorisations.nonEmpty) {
        Redirect(s"${config.vmvUrl}/search")
      } else {
        Ok(views.html.propertyLinking.beforeYouStart())
      }
    }
  }

  def showRelationship(uarn: Long, clientDetails: Option[ClientDetails] = None) =
    authenticatedAction.async { implicit request =>
      vmvConnector.getPropertyHistory(uarn).map { property =>
        Ok(
          relationshipToPropertyView(
            ClaimPropertyRelationshipVM(relationshipForm, property.addressFull, uarn),
            clientDetails = clientDetails,
            backLink(request)))
      }
    }

  private def backLink(request: Request[AnyContent]): String = {
    val link = request.headers.get("referer").getOrElse(config.dashboardUrl("home"))
    if (link.contains("/business-rates-find/valuations")) link else s"${config.vmvUrl}/back-to-list-valuations"
  }

  def submitRelationship(uarn: Long, clientDetails: Option[ClientDetails] = None): Action[AnyContent] =
    authenticatedAction.async { implicit request =>
      relationshipForm
        .bindFromRequest()
        .fold(
          errors =>
            vmvConnector.getPropertyHistory(uarn).map { property =>
              BadRequest(
                relationshipToPropertyView(
                  ClaimPropertyRelationshipVM(errors, property.addressFull, uarn),
                  clientDetails,
                  backLink(request)))
          },
          formData =>
            vmvConnector.getPropertyHistory(uarn).flatMap { property =>
              initialiseSession(formData, uarn, property.addressFull, clientDetails)
                .map { _ =>
                  Redirect(routes.ClaimPropertyOwnershipController.showOwnership())
                }
                .recover {
                  case UpstreamErrorResponse.Upstream5xxResponse(_) =>
                    ServiceUnavailable(views.html.errors.serviceUnavailable())
                }
          }
        )
    }

  def back: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession) { implicit request =>
    val form = request.ses.propertyRelationship.fold(relationshipForm) { relationship =>
      relationshipForm.fillAndValidate(relationship)
    }
    Ok(
      relationshipToPropertyView(
        ClaimPropertyRelationshipVM(form, request.ses.address, request.ses.uarn),
        request.ses.clientDetails,
        backLink(request)
      ))
  }

  private def initialiseSession(
        propertyRelationship: PropertyRelationship,
        uarn: Long,
        address: String,
        clientDetails: Option[ClientDetails])(implicit request: AuthenticatedRequest[_]): Future[Unit] =
    for {
      submissionId <- submissionIdConnector.get()
      _ <- sessionRepository.start[LinkingSession](
            LinkingSession(
              address = address,
              uarn = uarn,
              submissionId = submissionId,
              personId = request.personId,
              propertyRelationship = Some(propertyRelationship),
              None,
              clientDetails = clientDetails
            ))
    } yield ()

}

object ClaimPropertyRelationship {
  lazy val relationshipForm = Form(
    mapping(
      "capacity" -> EnumMapping(CapacityType)
    )(PropertyRelationship.apply)(PropertyRelationship.unapply))
}

case class ClaimPropertyRelationshipVM(form: Form[_], address: String, uarn: Long)
