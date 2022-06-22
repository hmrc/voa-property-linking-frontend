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

package uk.gov.hmrc.propertylinking.controllers.valuations

import java.net.URLEncoder
import java.time.LocalDate

import actions.AuthenticatedAction
import actions.assessments.WithAssessmentsPageSessionRefiner
import actions.assessments.request.AssessmentsPageSessionRequest
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{AssessmentsVM, PropertyLinkingController}
import javax.inject.{Inject, Named, Singleton}
import models.ApiAssessments.EmptyAssessments
import models.assessments.{AssessmentsPageSession, PreviousPage}
import models.properties.AllowedAction
import models.{ApiAssessment, ApiAssessments}
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

//TODO this should really sit inside business-rates-valuation-frontend

@Singleton
class ValuationsController @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: PropertyLinkConnector,
      authenticated: AuthenticatedAction,
      assessmentsView: views.html.dashboard.assessments,
      @Named("assessmentPage") val sessionRepo: SessionRepo,
      withAssessmentsPageSession: WithAssessmentsPageSessionRefiner,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig,
      executionContext: ExecutionContext
) extends PropertyLinkingController with Logging {

  def savePreviousPage(previousPage: String, submissionId: String, owner: Boolean): Action[AnyContent] =
    authenticated.async { implicit request =>
      sessionRepo
        .start[AssessmentsPageSession](AssessmentsPageSession(PreviousPage.withName(previousPage)))
        .map(_ =>
          Redirect(uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
            .valuations(submissionId, owner)))
    }

  private[controllers] def assessmentsWithLinks(
        apiAssessments: ApiAssessments,
        submissionId: String,
        owner: Boolean): Seq[(String, ApiAssessment)] = {
    val defaultEpochDay = LocalDate.of(2017, 4, 7).toEpochDay
    apiAssessments.assessments
      .sortBy(-_.currentFromDate.fold(defaultEpochDay)(_.toEpochDay))
      .collect {
        case a: ApiAssessment if a.allowedActions.contains(AllowedAction.VIEW_DETAILED_VALUATION) =>
          linkAndAssessment(submissionId, apiAssessments.authorisationId, a, owner)
      }
  }

  def valuations(submissionId: String, owner: Boolean): Action[AnyContent] =
    authenticated.andThen(withAssessmentsPageSession).async { implicit request =>
      val assessments: Future[Option[ApiAssessments]] = {
        if (owner)
          propertyLinks.getOwnerAssessments(submissionId)
        else
          propertyLinks.getClientAssessments(submissionId)
      }

      def okResponse(assessments: ApiAssessments, backlink: String): Result =
        Ok(
          assessmentsView(
            AssessmentsVM(
              assessmentsWithLinks(assessments, submissionId, owner),
              backlink,
              assessments.address,
              assessments.capacity),
            owner
          ))

      assessments
        .flatMap {
          case Some(EmptyAssessments()) | None => Future.successful(notFound)
          case Some(assessments) =>
            if (owner)
              Future.successful(
                okResponse(assessments, backlink = s"${config.dashboardUrl("return-to-your-properties")}"))
            else calculateBackLink(submissionId).map(backlink => okResponse(assessments, backlink))
        }
    }

  private def linkAndAssessment(
        submissionId: String,
        authorisationId: Long,
        assessment: ApiAssessment,
        owner: Boolean
  ): (String, ApiAssessment) =
    controllers.routes.Assessments
      .viewDetailedAssessment(
        submissionId,
        authorisationId,
        assessment.assessmentRef,
        assessment.billingAuthorityReference,
        owner)
      .url -> assessment

  private def calculateBackLink(
        submissionId: String)(implicit request: AssessmentsPageSessionRequest[_], hc: HeaderCarrier): Future[String] =
    request.sessionData match {
      case AssessmentsPageSession(PreviousPage.Dashboard) => Future.successful(config.dashboardUrl("home"))
      case AssessmentsPageSession(PreviousPage.AllClients) =>
        Future.successful(s"${config.dashboardUrl("return-to-client-properties")}")
      case AssessmentsPageSession(PreviousPage.SelectedClient) =>
        propertyLinks.clientPropertyLink(submissionId).map {
          case None =>
            throw new IllegalArgumentException(s"Client not fount for propertyLinkSubmissionId: $submissionId")
          case Some(clientPropertyLink) =>
            config.dashboardUrl(
              s"return-to-selected-client-properties?organisationId=${clientPropertyLink.client.organisationId}&organisationName=${URLEncoder
                .encode(clientPropertyLink.client.organisationName, "UTF-8")}")
        }
    }
}
