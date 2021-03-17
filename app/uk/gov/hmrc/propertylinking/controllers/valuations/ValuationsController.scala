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

package uk.gov.hmrc.propertylinking.controllers.valuations

import java.net.URLEncoder
import java.time.LocalDate

import actions.AuthenticatedAction
import actions.assessments.WithAssessmentsPageSessionRefiner
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{AssessmentsVM, PropertyLinkingController}
import javax.inject.{Inject, Named, Singleton}
import models.ApiAssessments.EmptyAssessments
import models.assessments.{AssessmentsPageSession, PreviousPage}
import models.{ApiAssessment, ApiAssessments}
import play.api.Logger
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
      @Named("assessmentPage") val sessionRepo: SessionRepo,
      withAssessmentsPageSession: WithAssessmentsPageSessionRefiner,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig,
      executionContext: ExecutionContext
) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  def savePreviousPage(previousPage: String, submissionId: String, owner: Boolean): Action[AnyContent] =
    authenticated.async { implicit request =>
      sessionRepo
        .start[AssessmentsPageSession](AssessmentsPageSession(PreviousPage.withName(previousPage)))
        .map(_ =>
          Redirect(uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
            .valuations(submissionId, owner)))
    }

  private[controllers] def assessmentsWithLinks(apiAssessments: ApiAssessments, submissionId: String, owner: Boolean) =
    apiAssessments.assessments
      .sortBy(-_.currentFromDate.fold(LocalDate.of(2017, 4, 7).toEpochDay)(_.toEpochDay))
      .map(decideNextUrl(submissionId, apiAssessments.authorisationId, _, apiAssessments.pending, owner))

  def valuations(submissionId: String, owner: Boolean): Action[AnyContent] =
    authenticated.andThen(withAssessmentsPageSession).async { implicit request =>
      val pLink: Future[Option[ApiAssessments]] = {
        if (owner)
          propertyLinks.getOwnerAssessments(submissionId)
        else
          propertyLinks.getClientAssessments(submissionId)
      }

      def okResponse(assessments: ApiAssessments, backlink: String) =
        Ok(
          views.html.dashboard.assessments(
            AssessmentsVM(
              assessmentsWithLinks(assessments, submissionId, owner),
              backlink,
              assessments.address,
              assessments.capacity),
            owner
          ))

      pLink
        .flatMap {
          case Some(EmptyAssessments()) | None => Future.successful(notFound)
          case Some(assessments) =>
            if (owner) Future.successful(okResponse(assessments, config.dashboardUrl("your-properties")))
            else calculateBackLink(submissionId).map(backlink => okResponse(assessments, backlink))
        }
        .recoverWith {
          case e =>
            logger.warn("property link assessment call failed", e)
            val linkF =
              if (owner) propertyLinks.getMyOrganisationPropertyLink(submissionId)
              else propertyLinks.getMyClientsPropertyLink(submissionId)
            linkF.map {
              case Some(link) => Redirect(getViewSummaryCall(link.uarn, pending = true, owner))
              case None       => notFound
            }
        }
    }

  private def getViewSummaryCall(uarn: Long, pending: Boolean, owner: Boolean): Call =
    if (owner) controllers.routes.Assessments.viewOwnerSummary(uarn, pending)
    else controllers.routes.Assessments.viewClientSummary(uarn, pending)

  private def decideNextUrl(
        submissionId: String,
        authorisationId: Long,
        assessment: ApiAssessment,
        isPending: Boolean,
        owner: Boolean
  ): (String, ApiAssessment) =
    assessment.rateableValue match {
      case Some(_) if isPending => getViewSummaryCall(assessment.uarn, isPending, owner).url -> assessment
      case _ =>
        controllers.routes.Assessments
          .viewDetailedAssessment(
            submissionId,
            authorisationId,
            assessment.assessmentRef,
            assessment.billingAuthorityReference,
            owner)
          .url -> assessment
    }

  private def calculateBackLink(submissionId: String)(implicit hc: HeaderCarrier): Future[String] =
    sessionRepo.get[AssessmentsPageSession].flatMap {
      case None => Future.successful(config.dashboardUrl("home"))
      case Some(sessionData) =>
        sessionData.previousPage match {
          case PreviousPage.AllClients => Future.successful(config.dashboardUrl("client-properties"))
          case PreviousPage.SelectedClient =>
            propertyLinks.clientPropertyLink(submissionId).map {
              case None =>
                throw new IllegalArgumentException(s"Client not fount for propertyLinkSubmissionId: $submissionId")
              case Some(clientPropertyLink) =>
                config.dashboardUrl(
                  s"selected-client-properties?clientOrganisationId=${clientPropertyLink.client.organisationId}&clientName=${URLEncoder
                    .encode(clientPropertyLink.client.organisationName, "UTF-8")}&pageNumber=1&pageSize=15&sortField=ADDRESS&sortOrder=ASC")
            }
        }
    }
}
