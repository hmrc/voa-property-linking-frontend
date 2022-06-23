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

package controllers

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.hmrc.propertylinking.services.PropertyLinkService

import scala.concurrent.{ExecutionContext, Future}

class Assessments @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinkService: PropertyLinkService,
      authenticated: AuthenticatedAction,
      businessRatesValuations: BusinessRatesValuationConnector,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig,
      executionContext: ExecutionContext
) extends PropertyLinkingController {

  def viewDetailedAssessment(
        submissionId: String,
        authorisationId: Long,
        assessmentRef: Long,
        baRef: String, // TODO remove - find which service links here (just Dashboard?)
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService.getSingularPropertyLink(submissionId, owner).flatMap {
      case Some(propertyLink) =>
        businessRatesValuations
          .isViewable(propertyLink.uarn, assessmentRef, submissionId)
          .map {
            case true =>
              if (owner) {
                Redirect(config.businessRatesValuationFrontendUrl(
                  s"property-link/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
              } else {
                Redirect(config.businessRatesValuationFrontendUrl(
                  s"property-link/clients/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
              }
            case false =>
              Redirect(
                if (owner)
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myOrganisationRequestDetailValuationCheck(submissionId, assessmentRef, propertyLink.uarn)
                else
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myClientsRequestDetailValuationCheck(submissionId, assessmentRef, propertyLink.uarn)
              )
          }
      case None => Future.successful(notFound)
    }
  }

}

case class AssessmentsVM(
      assessmentsWithLinks: Seq[(String, ApiAssessment)],
      backLink: String,
      address: String,
      capacity: Option[String]) {

  val localAuthorityReference = assessmentsWithLinks.headOption.map(_._2.billingAuthorityReference)
  val currentAssessments: Seq[(String, ApiAssessment)] =
    assessmentsWithLinks.filter(a =>
      a._2.listType == ListType.CURRENT && a._2.currentFromDate.nonEmpty && a._2.currentToDate.isEmpty)
  val draftAssessments: Seq[(String, ApiAssessment)] = assessmentsWithLinks.filter(a => a._2.listType == ListType.DRAFT)
  val historicAssessments: Seq[(String, ApiAssessment)] =
    assessmentsWithLinks.filterNot(currentAssessments.contains).filterNot(draftAssessments.contains)

}

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)
