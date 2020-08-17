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

package uk.gov.hmrc.propertylinking.controllers.valuations

import java.net.URLEncoder
import java.time.LocalDate

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{AssessmentsVM, PropertyLinkingController}
import javax.inject.{Inject, Named, Singleton}
import models.{ApiAssessment, ApiAssessments, ClientDetails}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.hmrc.propertylinking.services.PropertyLinkService

import scala.concurrent.{ExecutionContext, Future}

//TODO this should really sit inside business-rates-valuation-frontend

@Singleton
class ValuationsController @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: PropertyLinkConnector,
      propertyLinkService: PropertyLinkService,
      authenticated: AuthenticatedAction,
      override val controllerComponents: MessagesControllerComponents,
      @Named("detailed-valuation.skip") isSkipAssessment: Boolean
)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig,
      executionContext: ExecutionContext
) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  def valuations(submissionId: String, owner: Boolean): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink: Future[Option[ApiAssessments]] = {
      if (owner)
        propertyLinks.getOwnerAssessmentsWithCapacity(submissionId)
      else
        propertyLinks.getClientAssessmentsWithCapacity(submissionId)
    }

    (pLink flatMap {
      case Some(ApiAssessments(_, _, _, _, _, _, Seq(), _)) => Future.successful(notFound)
      case Some(link) =>
        if (!link.pending && link.assessments.size == 1 && isSkipAssessment) {
          Future.successful(
            Redirect(
              controllers.routes.Assessments.viewDetailedAssessment(
                submissionId,
                link.authorisationId,
                link.assessments.head.assessmentRef,
                link.assessments.head.billingAuthorityReference,
                owner)))
        } else if (link.pending && link.assessments.size == 1 && isSkipAssessment) {
          Future.successful(Redirect(getViewSummaryCall(link.uarn, link.pending, owner)))
        } else {
          if (owner) {
            Future.successful(
              Ok(
                views.html.dashboard.assessments(
                  model = AssessmentsVM(
                    assessmentsWithLinks = link.assessments
                      .sortBy(_.currentFromDate.getOrElse(LocalDate.of(2017, 4, 7)))(
                        Ordering.by[LocalDate, Long](_.toEpochDay))
                      .reverse
                      .map(decideNextUrl(submissionId, link.authorisationId, _, link.pending, owner)),
                    backLink = config.newDashboardUrl("your-properties"),
                    address = link.address,
                    capacity = link.capacity
                  ),
                  owner = owner
                ))
            )
          } else {
            propertyLinks.clientPropertyLink(submissionId).map {
              case None => notFound
              case Some(clientPropertyLink) =>
                Ok(views.html.dashboard.assessments(
                  model = AssessmentsVM(
                    assessmentsWithLinks = link.assessments
                      .sortBy(_.currentFromDate.getOrElse(LocalDate.of(2017, 4, 7)))(
                        Ordering.by[LocalDate, Long](_.toEpochDay))
                      .reverse
                      .map(decideNextUrl(submissionId, link.authorisationId, _, link.pending, owner)),
                    backLink = calculateBackLink(owner, Some(clientPropertyLink.client)),
                    address = link.address,
                    capacity = link.capacity
                  ),
                  owner = owner
                ))

            }
          }

        }
      case None => Future.successful(notFound)
    }).recoverWith {
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
  )(implicit request: Request[_]): (String, ApiAssessment) =
    assessment.rateableValue match {
      case None                 => getViewSummaryCall(assessment.uarn, isPending, owner).url -> assessment
      case Some(_) if isPending => getViewSummaryCall(assessment.uarn, isPending, owner).url -> assessment
      case Some(_) =>
        controllers.routes.Assessments
          .viewDetailedAssessment(
            submissionId,
            authorisationId,
            assessment.assessmentRef,
            assessment.billingAuthorityReference,
            owner)
          .url -> assessment
    }

  private def calculateBackLink(agentOwnsProperty: Boolean, clientDetails: Option[ClientDetails] = None): String =
    if (config.clientPropertiesEnabled) {
      clientDetails match {
        case Some(client) =>
          config.newDashboardUrl(
            s"selected-client-properties?clientOrganisationId=${client.organisationId}&clientName=${URLEncoder
              .encode(client.organisationName, "UTF-8")}&pageNumber=1&pageSize=15&sortField=ADDRESS&sortOrder=ASC")

        case _ => config.newDashboardUrl(if (!agentOwnsProperty) "client-properties" else "your-properties")
      }
    } else config.newDashboardUrl(if (!agentOwnsProperty) "client-properties" else "your-properties")
}
