/*
 * Copyright 2024 HM Revenue & Customs
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
import models._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.hmrc.propertylinking.services.PropertyLinkService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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
        owner: Boolean,
        otherValuationId: Option[Long] = None,
        fromValuation: Option[Long] = None,
        challengeCaseRef: Option[String] = None
  ): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService.getSingularPropertyLink(submissionId, owner).flatMap {
      case Some(propertyLink) =>
        businessRatesValuations
          .isViewable(propertyLink.uarn, assessmentRef, submissionId)
          .map {
            case true =>
              if (owner) {
                Redirect(config.businessRatesValuationFrontendUrl(
                  s"property-link/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId${Seq(
                    otherValuationId.fold("")("&otherValuationId=" + _),
                    fromValuation.fold("")("&fromValuation=" + _),
                    challengeCaseRef.fold("")("&challengeCaseRef=" + _)
                  ).mkString}"))
              } else {
                Redirect(config.businessRatesValuationFrontendUrl(
                  s"property-link/clients/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId${Seq(
                    otherValuationId.fold("")("&otherValuationId=" + _),
                    fromValuation.fold("")("&fromValuation=" + _),
                    challengeCaseRef.fold("")("&challengeCaseRef=" + _)
                  ).mkString}"))
              }
            case false =>
              Redirect(
                if (owner)
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myOrganisationRequestDetailValuationCheck(
                      submissionId,
                      assessmentRef,
                      otherValuationId = otherValuationId,
                      fromValuation = fromValuation,
                      challengeCaseRef = challengeCaseRef,
                      tabName = Some("valuation-tab")
                    )
                else
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myClientsRequestDetailValuationCheck(
                      submissionId,
                      assessmentRef,
                      otherValuationId = otherValuationId,
                      fromValuation = fromValuation,
                      challengeCaseRef = challengeCaseRef,
                      tabName = Some("valuation-tab")
                    )
              )
          }
      case None => notFound
    }
  }

}

case class AssessmentsVM(
      assessmentsWithLinks: Seq[(String, ApiAssessment)],
      backLink: String,
      address: String,
      capacity: Option[String],
      clientOrgName: Option[String]) {

  val localAuthorityReference = assessmentsWithLinks.headOption.map(_._2.billingAuthorityReference)
  val currentAssessments: Seq[(String, ApiAssessment)] =
    assessmentsWithLinks.filter(a =>
      a._2.listType == ListType.CURRENT && a._2.currentFromDate.nonEmpty && a._2.currentToDate.isEmpty)
  val draftAssessments: Seq[(String, ApiAssessment)] = assessmentsWithLinks.filter(a => a._2.listType == ListType.DRAFT)
  val historicAssessments: Seq[(String, ApiAssessment)] =
    assessmentsWithLinks.filterNot(currentAssessments.contains).filterNot(draftAssessments.contains)

}
