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

package controllers

import java.time.LocalDate

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Request}

import scala.concurrent.{ExecutionContext, Future}

class Assessments @Inject()(propertyLinks: PropertyLinkConnector, authenticated: AuthenticatedAction,
                            submissionIds: SubmissionIdConnector, dvrCaseManagement: DVRCaseManagementConnector,
                            businessRatesValuations: BusinessRatesValuationConnector,
                            businessRatesAuthorisation: BusinessRatesAuthorisation)
                           (implicit val messagesApi: MessagesApi, val config: ApplicationConfig, executionContext: ExecutionContext) extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def assessments(submissionId: String, owner: Boolean) = authenticated.async { implicit request =>
    val refererOpt = request.headers.get("Referer")

    val pLink: Future[Option[ApiAssessments]] = {
      if (owner)
        propertyLinks.getOwnerAssessmentsWithCapacity(submissionId)
      else
        propertyLinks.getClientAssessmentsWithCapacity(submissionId)
    }

    (pLink flatMap {
      case Some(ApiAssessments(authorisationId, _, _, _, _, _, Seq(), _)) => notFound
      case Some(link) =>
        if (!link.pending && link.assessments.size == 1) {
          Redirect(routes.Assessments.viewDetailedAssessment(submissionId, link.authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference, owner))
        } else if (link.pending && link.assessments.size == 1) {
          Redirect(routes.Assessments.viewSummary(link.uarn, link.pending))
        } else {
          Ok(
            views.html.dashboard.assessments(
              model = AssessmentsVM(
                form = viewAssessmentForm,
                assessmentsWithLinks =
                  link.assessments.sortBy(_.currentFromDate.getOrElse(LocalDate.of(2017, 4, 7)))(Ordering.by[LocalDate, Long](_.toEpochDay)).reverse
                    .map(decideNextUrl(submissionId, link.authorisationId, _, link.pending, owner)),
                backLink = calculateBackLink(refererOpt, owner),
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
        val linkF = if(owner) propertyLinks.getMyOrganisationPropertyLink(submissionId) else propertyLinks.getMyClientsPropertyLink(submissionId)
        linkF.map {
          case Some(link) => Redirect(routes.Assessments.viewSummary(link.uarn, true))
          case None       => notFound
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
      case None                   => routes.Assessments.viewSummary(assessment.uarn, isPending).url -> assessment
      case Some(_) if isPending   => routes.Assessments.viewSummary(assessment.uarn, isPending).url -> assessment
      case Some(_)                => routes.Assessments.viewDetailedAssessment(submissionId, authorisationId, assessment.assessmentRef, assessment.billingAuthorityReference, owner).url -> assessment
    }
  }

  private def calculateBackLink(refererOpt: Option[String], agentOwnsProperty: Boolean): String = refererOpt match {
    case Some(referer) => referer
    case None => config.newDashboardUrl(if(!agentOwnsProperty) "client-properties" else "your-properties")
  }

  def viewSummary(uarn: Long, isPending: Boolean = false) = Action { implicit request =>
    Redirect(config.vmvUrl + s"/detail/$uarn?isPending=$isPending")
  }

  def viewDetailedAssessment(
                              submissionId: String,
                              authorisationId: Long,
                              assessmentRef: Long,
                              baRef: String,
                              owner: Boolean
                            ) = authenticated.async { implicit request =>
    businessRatesValuations.isViewable(authorisationId, assessmentRef) map {
      case true =>
        if (owner) {
          Redirect(config.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))

        } else {
          Redirect(config.businessRatesValuationUrl(s"property-link/clients/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
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

  lazy val viewAssessmentForm = Form(Forms.single("nextUrl" -> text))

    def submitViewAssessment(authorisationId: Long, submissionId: String, owner: Boolean) = authenticated.async { implicit request =>

    viewAssessmentForm.bindFromRequest().fold(
      errors => {
        val pLink = if(request.organisationAccount.isAgent) propertyLinks.getClientAssessmentsWithCapacity(submissionId) else propertyLinks.getOwnerAssessmentsWithCapacity(submissionId)

        pLink flatMap {
            case Some(ApiAssessments(_, _, _, _, _,_,Seq(), _)) => notFound
            case Some(link) => {
              for {
                isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
              } yield {
                Ok(views.html.dashboard.assessments(
                  AssessmentsVM(
                    form = errors,
                    assessmentsWithLinks = link.assessments.map(decideNextUrl(submissionId, authorisationId, _, link.pending, owner)),
                    backLink = calculateBackLink(request.headers.get("Referer"), isAgentOwnProperty),
                    authorisationId = link.authorisationId,
                    linkPending = link.pending,
                    address = link.address,
                    plSubmissionId = link.submissionId,
                    isAgentOwnProperty = isAgentOwnProperty,
                    capacity = link.capacity), owner))
              }
            }
            case None => notFound
          }
      },
      redirectUrl => Future.successful(Redirect(redirectUrl))
    )
  }

  def startChallengeFromDVR(submissionId: String, valuationId: Long, owner: Boolean) = authenticated.async { implicit request =>
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