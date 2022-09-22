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

package controllers.propertyLinking

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import actions.requests.AuthenticatedRequest
import binders.propertylinks.GetPropertyLinksParameters
import binders.propertylinks.ClaimPropertyReturnToPage._
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.SubmissionIdConnector
import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import controllers._
import form.EnumMapping
import models._
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.propertylinking.PropertyLinkingService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimPropertyRelationshipController @Inject()(
      val errorHandler: CustomErrorHandler,
      val submissionIdConnector: SubmissionIdConnector,
      @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      propertyLinkingService: PropertyLinkingService,
      val propertyLinksConnector: PropertyLinkConnector,
      val vmvConnector: VmvConnector,
      val runModeConfiguration: Configuration,
      relationshipToPropertyView: views.html.propertyLinking.relationshipToProperty,
      beforeYouStartView: views.html.propertyLinking.beforeYouStart)(
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
        Ok(beforeYouStartView())
      }
    }
  }

  def showRelationship(
        uarn: Long,
        clientDetails: Option[ClientDetails] = None,
        rtp: ClaimPropertyReturnToPage,
        valuationId: Option[Long] = None): Action[AnyContent] =
    authenticatedAction.async { implicit request =>
      for {
        property <- vmvConnector.getPropertyHistory(uarn)
        _        <- initialiseSession(uarn, clientDetails, rtp, valuationId)
      } yield
        Ok(
          relationshipToPropertyView(
            ClaimPropertyRelationshipVM(relationshipForm, property.addressFull, uarn, property.localAuthorityReference),
            clientDetails = clientDetails,
            backLinkToVmv(rtp, uarn, valuationId)
          ))
    }

  def back: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession) { implicit request =>
    val form = request.ses.propertyRelationship.fold(relationshipForm) { relationship =>
      relationshipForm.fillAndValidate(relationship)
    }
    Ok(
      relationshipToPropertyView(
        ClaimPropertyRelationshipVM(form, request.ses.address, request.ses.uarn, request.ses.localAuthorityReference),
        request.ses.clientDetails,
        getBackLink
      ))
  }

  def submitRelationship(uarn: Long, clientDetails: Option[ClientDetails] = None): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      relationshipForm
        .bindFromRequest()
        .fold(
          errors =>
            vmvConnector.getPropertyHistory(uarn).map { property =>
              BadRequest(
                relationshipToPropertyView(
                  ClaimPropertyRelationshipVM(errors, property.addressFull, uarn, property.localAuthorityReference),
                  clientDetails,
                  getBackLink
                ))
          },
          formData =>
            sessionRepository
              .saveOrUpdate[LinkingSession](request.ses.copy(propertyRelationship = Some(formData)))
              .map { _ =>
                if (request.ses.fromCya.contains(true)) Redirect(routes.DeclarationController.show())
                else Redirect(routes.ClaimPropertyOwnershipController.showOwnership())
            }
        )
    }

  private def backLinkToVmv(rtp: ClaimPropertyReturnToPage, uarn: Long, valuationId: Option[Long]): String = {
    val valuationIdPart = valuationId.fold("")(id => s"?valuationId=$id")
    rtp match {
      case FMBR                 => s"${config.vmvUrl}/back-to-list-valuations"
      case SummaryValuation     => s"${config.vmvUrl}/valuations/$uarn$valuationIdPart"
      case SummaryValuationHelp => s"${config.vmvUrl}/valuations/$uarn$valuationIdPart#help-tab"
    }
  }

  private def getBackLink(implicit request: LinkingSessionRequest[_]): String =
    if (request.ses.fromCya.contains(true)) routes.DeclarationController.show().url
    else backLinkToVmv(request.ses.rtp, request.ses.uarn, valuationId = request.ses.valuationId)

  private def initialiseSession(
        uarn: Long,
        clientDetails: Option[ClientDetails],
        rtp: ClaimPropertyReturnToPage,
        valuationId: Option[Long])(implicit request: AuthenticatedRequest[_]): Future[Unit] =
    for {
      propertyHistory <- vmvConnector.getPropertyHistory(uarn)
      submissionId    <- submissionIdConnector.get()
      earliestStartDate = propertyLinkingService.findEarliestStartDate(propertyHistory)
      _ <- sessionRepository.start[LinkingSession](
            LinkingSession(
              address = propertyHistory.addressFull,
              uarn = uarn,
              submissionId = submissionId,
              personId = request.personId,
              earliestStartDate = earliestStartDate,
              propertyRelationship = None,
              propertyOwnership = None,
              propertyOccupancy = None,
              hasRatesBill = None,
              clientDetails = clientDetails,
              localAuthorityReference = propertyHistory.localAuthorityReference,
              rtp = rtp,
              valuationId = valuationId,
              fromCya = Some(false)
            ))
    } yield ()

}

object ClaimPropertyRelationship {
  lazy val relationshipForm = Form(
    mapping(
      "capacity" -> EnumMapping(CapacityType)
    )(PropertyRelationship.apply)(PropertyRelationship.unapply))
}

case class ClaimPropertyRelationshipVM(form: Form[_], address: String, uarn: Long, localAuthorityReference: String)
