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
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.Future

class Assessments @Inject()(propertyLinks: PropertyLinkConnector, authenticated: AuthenticatedAction,
                            submissionIds: SubmissionIdConnector, dvrCaseManagement: DVRCaseManagementConnector,
                            businessRatesValuations: BusinessRatesValuationConnector,
                            businessRatesAuthorisation: BusinessRatesAuthorisation)
                           (implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def assessments(submissionId: String, owner: Boolean) = authenticated.async { implicit request =>
    val refererOpt = request.headers.get("Referer")

    val pLink = if(owner) propertyLinks.getOwnerAssessmentsWithCapacity(submissionId) else propertyLinks.getClientAssessmentsWithCapacity(submissionId)

        pLink flatMap {
          case Some(ApiAssessments(authorisationId, _, _, _, _, _, Seq(), _)) => notFound
          case Some(link) => {
              if (!link.pending && link.assessments.size == 1) {
                Redirect(routes.Assessments.viewDetailedAssessment(submissionId, link.authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference, owner))
              }
              else if (link.pending && link.assessments.size == 1) {
                Redirect(routes.Assessments.viewSummary(link.uarn, link.pending))
              }
              else {
                Ok(
                  views.html.dashboard.assessments(
                    model = AssessmentsVM(
                      form = viewAssessmentForm,
                      assessments = link.assessments.sortBy(_.currentFromDate.getOrElse(LocalDate.of(2017, 4, 7)))(Ordering.by[LocalDate, Long](_.toEpochDay)).reverse,
                      backLink = calculateBackLink(refererOpt, owner),
                      linkPending = link.pending,
                      plSubmissionId = link.submissionId,
                      isAgentOwnProperty = owner,
                      capacity = link.capacity),
                    owner = owner
                  ))
              }
            }
          case None => notFound
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
            controllers.detailedValuationRequest.routes.DvrController.myOrganisationRequestDetailValuationCheck(submissionId, assessmentRef)
          else
            controllers.detailedValuationRequest.routes.DvrController.myClientsRequestDetailValuationCheck(submissionId, assessmentRef)
        )

    }
  }

  lazy val viewAssessmentForm = Form(Forms.single("viewAssessmentRadio" -> text.transform[(String, Long, String)](x => x.split("-").toList match {
    case uarn :: assessmentRef :: baRef :: Nil => (uarn, assessmentRef.toLong, baRef)
  }, y => s"${y._1}-${y._2}-${y._3}")))

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
                    viewAssessmentForm,
                    link.assessments,
                    calculateBackLink(request.headers.get("Referer"), isAgentOwnProperty),
                    link.pending,
                    plSubmissionId = link.submissionId,
                    isAgentOwnProperty,
                    capacity = link.capacity), owner))
              }
            }
            case None => notFound
          }
      }
      ,
      {
        case (uarn, assessmentRef, baRef) =>
          uarn match {
            case "" => Future.successful(Redirect(routes.Assessments.viewDetailedAssessment(submissionId, authorisationId, assessmentRef, baRef, owner)))
            case _ =>
              val pLink = if(request.organisationAccount.isAgent) propertyLinks.getClientAssessments(submissionId) else propertyLinks.getOwnerAssessments(submissionId)
              pLink flatMap {
                case Some(ApiAssessments(_, _, _, _, _, _,Seq(), _)) => Future.successful(Redirect(routes.Assessments.viewSummary(uarn.toLong, false)))
                case Some(link) => Future.successful(Redirect(routes.Assessments.viewSummary(uarn.toLong, link.pending)))
                case None => Future.successful(Redirect(routes.Assessments.viewSummary(uarn.toLong, false)))
              }
          }
      }
    )
  }

  def startChallengeFromDVR(submissionId: String, valuationId: Long, owner: Boolean) = authenticated.async { implicit request =>
    Ok(views.html.dvr.challenge_valuation(submissionId, valuationId, owner))
  }
}

case class AssessmentsVM(form: Form[_], assessments: Seq[ApiAssessment], backLink: String, linkPending: Boolean, plSubmissionId: String, isAgentOwnProperty: Boolean, capacity: Option[String])

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)