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

package utils

import connectors.IdentityVerificationConnector
import models.identityVerificationProxy.IvResult
import models.identityVerificationProxy.IvResult.{IvFailure, IvSuccess}
import org.mockito.Mockito
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import utils.Configs._

import scala.concurrent.{ExecutionContext, Future}

object StubIdentityVerification
    extends IdentityVerificationConnector(servicesConfig, null, Mockito.mock(classOf[HttpClient]))(
      ExecutionContext.global) {

  private var journeyResult: (String, IvResult) = ("", IvSuccess)

  def stubSuccessfulJourney(id: String) = journeyResult = (id, IvSuccess)

  def stubFailedJourney(id: String, ivFailure: IvFailure = IvFailure.FailedIV) = journeyResult = (id, ivFailure)

  def reset() = journeyResult = ("", IvSuccess)

  override def verifySuccess(journeyId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    Future.successful(journeyResult._1 == journeyId && journeyResult._2 == IvSuccess)

  override def journeyStatus(journeyId: String)(implicit hc: HeaderCarrier): Future[IvResult] =
    Future.successful(journeyResult._2)

}
