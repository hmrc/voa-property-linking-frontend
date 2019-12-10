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

package controllers

import java.time.LocalDate

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.authorisation.BusinessRatesAuthorisationConnector
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.{Inject, Named}
import models._
import play.api.Logger
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.voa.propertylinking.services.PropertyLinkService

import scala.concurrent.{ExecutionContext, Future}

class Assessments @Inject()(
                             val errorHandler: CustomErrorHandler,
                             propertyLinks: PropertyLinkConnector,
                             propertyLinkService: PropertyLinkService,
                             authenticated: AuthenticatedAction,
                             submissionIds: SubmissionIdConnector,
                             dvrCaseManagement: DVRCaseManagementConnector,
                             businessRatesValuations: BusinessRatesValuationConnector,
                             businessRatesAuthorisation: BusinessRatesAuthorisationConnector,
                             override val controllerComponents: MessagesControllerComponents,
                             @Named("detailed-valuation.external") isExternalValuation: Boolean,
                             @Named("detailed-valuation.skip") isSkipAssessment: Boolean,
                             @Named("summary-valuation.newRoute") isSummaryValuationNewRoute: Boolean
                           )(
                             implicit override val messagesApi: MessagesApi,
                             val config: ApplicationConfig,
                             executionContext: ExecutionContext
                           ) extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def assessments(submissionId: String, owner: Boolean): Action[AnyContent] = authenticated.async { implicit request =>

    val pLink: Future[Option[ApiAssessments]] = {
      if (owner)
        propertyLinks.getOwnerAssessmentsWithCapacity(submissionId)
      else
        propertyLinks.getClientAssessmentsWithCapacity(submissionId)
    }

    (pLink map {
      case Some(ApiAssessments(authorisationId, _, _, _, _, _, Seq(), _)) => notFound
      case Some(link) =>
        if (!link.pending && link.assessments.size == 1 && isSkipAssessment) {
          Redirect(routes.Assessments.viewDetailedAssessment(submissionId, link.authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference, owner))
        } else if (link.pending && link.assessments.size == 1 && isSkipAssessment) {
          Redirect(routes.Assessments.viewSummary(link.uarn, link.assessments.head.assessmentRef, link.pending))
        } else {
          Ok(
            views.html.dashboard.assessments(
              model = AssessmentsVM(
                form = viewAssessmentForm,
                assessmentsWithLinks =
                  link.assessments.sortBy(_.currentFromDate.getOrElse(LocalDate.of(2017, 4, 7)))(Ordering.by[LocalDate, Long](_.toEpochDay)).reverse
                    .map(decideNextUrl(submissionId, link.authorisationId, _, link.pending, owner)),
                backLink = calculateBackLink(owner),
                linkPending = link.pending,
                authorisationId = link.authorisationId,
                address = link.address,
                plSubmissionId = link.submissionId,
                isAgentOwnProperty = owner,
                capacity = link.capacity),
              owner = owner
            ))
        }
      case None => notFound
    }).recoverWith {
      case e =>
        logger.warn("property link assessment call failed", e)
        val linkF = if (owner) propertyLinks.getMyOrganisationPropertyLink(submissionId) else propertyLinks.getMyClientsPropertyLink(submissionId)
        linkF.map {
          // TODO what do we do in this scenario
          case Some(link) => Redirect(routes.Assessments.viewSummary(link.uarn, 1L , true))
          case None => notFound
        }
    }
  }

  private def decideNextUrl(
                             submissionId: String,
                             authorisationId: Long,
                             assessment: ApiAssessment,
                             isPending: Boolean,
                             owner: Boolean
                           )(implicit request: Request[_]): (String, ApiAssessment) = {
    assessment.rateableValue match {
      case None => routes.Assessments.viewSummary(assessment.uarn, assessment.assessmentRef, isPending).url -> assessment
      case Some(_) if isPending => routes.Assessments.viewSummary(assessment.uarn, assessment.assessmentRef, isPending).url -> assessment
      case Some(_) => routes.Assessments.viewDetailedAssessment(submissionId, authorisationId, assessment.assessmentRef, assessment.billingAuthorityReference, owner).url -> assessment
    }
  }

  private def calculateBackLink(agentOwnsProperty: Boolean): String = {
    config.newDashboardUrl(if (!agentOwnsProperty) "client-properties" else "your-properties")
  }

  def viewSummary(uarn: Long, assessmentRef: Long, isPending: Boolean = false) = Action { implicit request =>
    if(isSummaryValuationNewRoute) {
      Redirect(config.valuationFrontendUrl + s"/summary/$assessmentRef/$uarn")
    }
    else {
      Redirect(config.vmvUrl + s"/detail/$uarn?isPending=$isPending")
    }
  def viewSummary(uarn: Long, isPending: Boolean = false): Action[AnyContent] = Action { implicit request =>
    Redirect(config.vmvUrl + s"/detail/$uarn?isPending=$isPending")
  }

  def viewDetailedAssessment(
                              submissionId: String,
                              authorisationId: Long,
                              assessmentRef: Long,
                              baRef: String,
                              owner: Boolean
                            ): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService.getSingularPropertyLink(submissionId, owner).flatMap {
      case Some(propertyLink) =>
        if (isExternalValuation) {
          businessRatesValuations.isViewableExternal(propertyLink.uarn, assessmentRef, submissionId)
            .map {
            case true =>
              if (owner) {
                if (isExternalValuation) {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                } else {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                }
              } else {
                if (isExternalValuation) {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/clients/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                } else {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/clients/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                }
              }
            case false =>
              Redirect(
                if (owner)
                  controllers.detailedvaluationrequest.routes.DvrController.myOrganisationRequestDetailValuationCheck(submissionId, assessmentRef)
                else
                  controllers.detailedvaluationrequest.routes.DvrController.myClientsRequestDetailValuationCheck(submissionId, assessmentRef)
              )
          }
        } else {
          businessRatesValuations.isViewable(propertyLink.uarn, assessmentRef, propertyLink.authorisationId)
            .map {
            case true =>
              if (owner) {
                if (isExternalValuation) {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                } else {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                }
              } else {
                if (isExternalValuation) {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/clients/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                } else {
                  Redirect(config.businessRatesValuationFrontendUrl(s"property-link/clients/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                }
              }
            case false =>
              Redirect(
                if (owner)
                  controllers.detailedvaluationrequest.routes.DvrController.myOrganisationRequestDetailValuationCheck(submissionId, assessmentRef)
                else
                  controllers.detailedvaluationrequest.routes.DvrController.myClientsRequestDetailValuationCheck(submissionId, assessmentRef)
              )
          }
        }
      case None => Future.successful(notFound)
    }
  }

  lazy val viewAssessmentForm = Form(Forms.single("nextUrl" -> text))

  def submitViewAssessment(authorisationId: Long, submissionId: String, owner: Boolean) = authenticated.async { implicit request =>

    viewAssessmentForm.bindFromRequest().fold(
      errors => {
        val pLink = if (request.organisationAccount.isAgent) propertyLinks.getClientAssessmentsWithCapacity(submissionId) else propertyLinks.getOwnerAssessmentsWithCapacity(submissionId)

        pLink flatMap {
          case Some(ApiAssessments(_, _, _, _, _, _, Seq(), _)) | None => Future.successful(notFound)
          case Some(link) => {
            for {
              isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
            } yield {
              Ok(views.html.dashboard.assessments(
                AssessmentsVM(
                  form = errors,
                  assessmentsWithLinks = link.assessments.map(decideNextUrl(submissionId, authorisationId, _, link.pending, owner)),
                  backLink = calculateBackLink(isAgentOwnProperty),
                  authorisationId = link.authorisationId,
                  linkPending = link.pending,
                  address = link.address,
                  plSubmissionId = link.submissionId,
                  isAgentOwnProperty = isAgentOwnProperty,
                  capacity = link.capacity), owner))
            }
          }
        }
      },
      redirectUrl => Future.successful(Redirect(redirectUrl))
    )
  }

  def startChallengeFromDVR(
                             submissionId: String,
                             valuationId: Long,
                             owner: Boolean
                           ): Action[AnyContent] = authenticated { implicit request =>
    Ok(views.html.dvr.challenge_valuation(submissionId, valuationId, owner))
  }
}

case class AssessmentsVM(
                          form: Form[_],
                          assessmentsWithLinks: Seq[(String, ApiAssessment)],
                          backLink: String,
                          linkPending: Boolean,
                          address: String,
                          authorisationId: Long,
                          plSubmissionId: String,
                          isAgentOwnProperty: Boolean,
                          capacity: Option[String]
                        )

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)