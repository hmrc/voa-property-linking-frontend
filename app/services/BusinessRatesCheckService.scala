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

package services

import connectors.check.BusinessRatesCheckConnector
import models.check.{CheckId, CheckIdWrapper}
import models.dvr.cases.check.CheckType
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.businessrates.values.{AssessmentRef, PropertyLinkId}
import uk.gov.voa.businessrates.values.connectors.RequestResult._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesCheckService @Inject()(
      val businessRatesCheckConnector: BusinessRatesCheckConnector,
      implicit val ec: ExecutionContext
) {

  def start(
        propertyLinkId: PropertyLinkId,
        assessmentRef: AssessmentRef,
        checkType: CheckType,
        propertyLinkSubmissionId: Option[String] = None,
        uarn: Option[Long] = None,
        dvrCheck: Boolean = false,
        rateableValueTooHigh: Option[Boolean] = None
  )(implicit hc: HeaderCarrier): Future[Either[RequestFailure, CheckId]] =
    businessRatesCheckConnector
      .start(propertyLinkId, assessmentRef, checkType, propertyLinkSubmissionId, uarn, dvrCheck, rateableValueTooHigh)
      .map {
        case Right(response) =>
          Right(response.json.as[CheckIdWrapper].id)
        case Left(failure) => Left(failure)
      }

  def updateResumeCheckUrl(checkId: CheckId, resumeUrl: String)(
        implicit hc: HeaderCarrier
  ): Future[Either[RequestFailure, RequestSuccess]] =
    businessRatesCheckConnector.updateResumeCheckUrl(checkId, resumeUrl).map {
      case Right(_)      => Right(Success)
      case Left(failure) => Left(failure)
    }

}
