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

package utils

import connectors.BusinessRatesValuationConnector
import org.mockito.Mockito.mock
import uk.gov.hmrc.http.HttpClient
import utils.Configs._

import scala.concurrent.ExecutionContext

object StubBusinessRatesValuation
    extends BusinessRatesValuationConnector(servicesConfig, mock(classOf[HttpClient]))(ExecutionContext.global) {
  private var stubbedValuations: Map[Long, Boolean] = Map()

  def stubValuation(assessmentRef: Long, isViewable: Boolean) =
    stubbedValuations = stubbedValuations.updated(assessmentRef, isViewable)

  def reset() =
    stubbedValuations = Map()
}
