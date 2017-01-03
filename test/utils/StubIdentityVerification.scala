/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.IdentityVerification
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubIdentityVerification extends IdentityVerification(StubHttp) {

  private var journeyResult = ("", "")

  def stubSuccessfulJourney(id: String) = journeyResult = (id, "Success")
  def stubFailedJourney(id: String) = journeyResult = (id, "FailedIV")
  def reset() = journeyResult = ("", "")

  override def verifySuccess(journeyId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    Future.successful(journeyResult._1 == journeyId && journeyResult._2 == "Success")
  }
}
