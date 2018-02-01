/*
 * Copyright 2018 HM Revenue & Customs
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
import javax.inject.Inject
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import form.EnumMapping
import models._
import play.api.data.{Form, Forms}
import play.api.mvc.Action

class Assessments @Inject()(config: ApplicationConfig,
                            propertyLinks: PropertyLinkConnector,
                            authenticated: AuthenticatedAction,
                            submissionIds: SubmissionIdConnector,
                            dvrCaseManagement: DVRCaseManagementConnector,
                            businessRatesValuations: BusinessRatesValuationConnector) extends PropertyLinkingController {

  def assessments(authorisationId: Long) = authenticated.toViewAssessmentsFor(authorisationId) { implicit request =>
    val backLink = request.headers.get("Referer")

    propertyLinks.getLink(authorisationId) map {
      case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound // TODO update page to handle no assessments
      case Some(link) => Ok(views.html.dashboard.assessments(AssessmentsVM(link.assessments, backLink, link.pending)))
      case None => notFound
    }
  }

  def viewSummary(uarn: Long) = Action { implicit request =>
    Redirect(config.vmvUrl + s"/detail/$uarn")
  }

  def viewDetailedAssessment(authorisationId: Long, assessmentRef: Long, baRef: String) = authenticated { implicit request =>
    businessRatesValuations.isViewable(authorisationId, assessmentRef) map {
      case true => Redirect(config.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef"))
      case false => Redirect(routes.Assessments.requestDetailedValuation(authorisationId, assessmentRef, baRef))
    }
  }

  def requestDetailedValuation(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Ok(views.html.dvr.requestDetailedValuation(RequestDetailedValuationVM(dvRequestForm, authId, assessmentRef, baRef)))
  }

  def startChallengeFromDVR = authenticated { implicit request =>
    Ok(views.html.dvr.startChallenge())
  }

  def detailedValuationRequested(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    dvRequestForm.bindFromRequest().fold(
      errs => BadRequest(views.html.dvr.requestDetailedValuation(RequestDetailedValuationVM(errs, authId, assessmentRef, baRef))),
      preference => {
        val prefix = preference match {
          case EmailRequest => "EMAIL"
          case PostRequest => "POST"
        }

        for {
          submissionId <- submissionIds.get(prefix)
          dvr = DetailedValuationRequest(authId, request.organisationId, request.personId, submissionId, assessmentRef, baRef)
          _ <- dvrCaseManagement.requestDetailedValuation(dvr)
        } yield {
          Redirect(routes.Assessments.dvRequestConfirmation(submissionId))
        }
      }
    )
  }

  def dvRequestConfirmation(submissionId: String) = Action { implicit request =>
    val preference = if (submissionId.startsWith("EMAIL")) "email" else "post"
    Ok(views.html.dvr.detailedValuationRequested(submissionId, preference))
  }

  lazy val dvRequestForm = Form(Forms.single("requestType" -> EnumMapping(DetailedValuationRequestTypes)))
}

case class AssessmentsVM(assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean)

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)