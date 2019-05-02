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

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import exceptionhandler.ErrorHandler
import form.EnumMapping
import javax.inject.Inject
import models._
import models.dvr.{DetailedValuationRequest, DetailedValuationRequestTypes, EmailRequest, PostRequest}
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.Future

class Assessments @Inject()(
                             propertyLinks: PropertyLinkConnector,
                             authenticated: AuthenticatedAction,
                             submissionIds: SubmissionIdConnector,
                             dvrCaseManagement: DVRCaseManagementConnector,
                             businessRatesValuations: BusinessRatesValuationConnector,
                             businessRatesAuthorisation: BusinessRatesAuthorisation,
                             errorHandler: ErrorHandler
                           )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def assessments(authorisationId: Long) = authenticated.toViewAssessmentsFor(authorisationId) { implicit request =>
    val backLink = request.headers.get("Referer")

      propertyLinks.getLink(authorisationId) flatMap {
        case PropertyLink(_, _, _, _, _, _, _, _, Seq(), _) => Future.successful(errorHandler.notFound)
        case link => {
          for {
            isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
          } yield {
            if(!link.pending && link.assessments.size == 1){
              Redirect(routes.Assessments.viewDetailedAssessment(authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference))
            }
            else if(link.pending && link.assessments.size == 1){
              Redirect(routes.Assessments.viewSummary(link.uarn, link.pending))
            }
            else Ok(views.html.dashboard.assessments(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, plSubmissionId = link.submissionId, isAgentOwnProperty)))
          }
        }
      }

  }

  def viewSummary(uarn: Long, isPending: Boolean = false) = Action { implicit request =>
    Redirect(config.vmvUrl + s"/detail/$uarn?isPending=$isPending")
  }

  def viewDetailedAssessment(authorisationId: Long, assessmentRef: Long, baRef: String) = authenticated { implicit request =>
    businessRatesValuations.isViewable(authorisationId, assessmentRef) map {
      case true => Redirect(config.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef"))
      case false =>
        if (config.dvrEnabled){
          Redirect(routes.DvrController.detailedValuationRequestCheck(authorisationId, assessmentRef, baRef))
        } else {
          Redirect(routes.Assessments.requestDetailedValuation(authorisationId, assessmentRef, baRef))
        }
    }
  }

  lazy val viewAssessmentForm = Form(Forms.single("viewAssessmentRadio" -> text.transform[(String, Long, String)](x => x.split("-").toList match {
    case uarn :: assessmentRef :: baRef :: Nil => (uarn, assessmentRef.toLong, baRef)
  }, y => s"${y._1}-${y._2}-${y._3}")))

  def submitViewAssessment(authorisationId: Long) = authenticated { implicit request =>
    val backLink = request.headers.get("Referer")
    viewAssessmentForm.bindFromRequest().fold(
      errors => {
          propertyLinks.getLink(authorisationId) flatMap {
            case PropertyLink(_, _, _, _, _, _, _, _, Seq(), _) => Future.successful(errorHandler.notFound)
            case link => {
              for {
                isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
              } yield {
                Ok(views.html.dashboard.assessments(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, plSubmissionId = link.submissionId, isAgentOwnProperty)))
              }
            }
          }
      }
      ,
      {
        case (uarn, assessmentRef, baRef) =>
          uarn match {
            case "" => Future.successful(Redirect(routes.Assessments.viewDetailedAssessment(authorisationId, assessmentRef, baRef)))
            case _ =>
              propertyLinks.getLink(authorisationId) flatMap {
                case PropertyLink(_, _, _, _, _, _, _, _, Seq(), _) => Future.successful(Redirect(routes.Assessments.viewSummary(uarn.toLong, false)))
                case link => Future.successful(Redirect(routes.Assessments.viewSummary(uarn.toLong, link.pending)))
              }
          }
      }
    )
  }

  def requestDetailedValuation(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Future.successful(Ok(views.html.dvr.requestDetailedValuation(RequestDetailedValuationVM(dvRequestForm, authId, assessmentRef, baRef))))
  }

  def duplicateRequestDetailedValuation(authId: Long, assessmentRef: Long) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Future.successful(Ok(views.html.dvr.duplicateRequestDetailedValuation()))
  }

  def startChallengeFromDVR(authorisationId: Long, assessmentRef: Long, baRef: String) = authenticated { implicit request =>
    Future.successful(Ok(views.html.dvr.challenge_valuation(authorisationId, assessmentRef, baRef)))
  }

  def detailedValuationRequested(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    dvRequestForm.bindFromRequest().fold(
      errs => Future.successful(BadRequest(views.html.dvr.requestDetailedValuation(RequestDetailedValuationVM(errs, authId, assessmentRef, baRef)))),
      preference => {
        val prefix = preference match {
          case EmailRequest => "EMAIL"
          case PostRequest => "POST"
        }

        for {
          submissionId  <- submissionIds.get(prefix)
          dvr           = DetailedValuationRequest(authId, request.organisationId, request.personId, submissionId, assessmentRef, Nil, baRef)
          dvrExists     <- dvrCaseManagement.dvrExists(request.organisationId, assessmentRef)
          _             <- if (!dvrExists) dvrCaseManagement.requestDetailedValuation(dvr) else Future.successful(())
        } yield {
          if (dvrExists) {
            Redirect(routes.Assessments.duplicateRequestDetailedValuation(authId, assessmentRef))
          } else {
            Redirect(routes.Assessments.dvRequestConfirmation(submissionId, authId))
          }
        }
      }
    )
  }

  def dvRequestConfirmation(submissionId: String, authorisationId: Long) = Action.async { implicit request =>
    val preference = if (submissionId.startsWith("EMAIL")) "email" else "post"
    propertyLinks.getLink(authorisationId).map { link =>
      Ok(views.html.dvr.detailedValuationRequested(submissionId, preference, link.address))
    }
  }

  private def getPaperChallengeUrl(assessmentSeq: Seq[Assessment]): String = {
    val a = assessmentSeq.sortWith(_.effectiveDate.toEpochDay < _.effectiveDate.toEpochDay).head
    config.businessRatesValuationUrl(s"property-link/${a.authorisationId}/assessment/${a.assessmentRef}/startChallenge")
  }

  lazy val dvRequestForm = Form(Forms.single("requestType" -> EnumMapping(DetailedValuationRequestTypes)))
}

case class AssessmentsVM(form: Form[_], assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean, plSubmissionId: String, isAgentOwnProperty: Boolean)

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)