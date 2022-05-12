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

import connectors.PropertyRepresentationConnector
import models.searchApi.{AgentAuthResult, AgentAuthorisation}
import org.mockito.Mockito.mock
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import utils.Configs._

import scala.concurrent.{ExecutionContext, Future}

object StubPropertyRepresentationConnector
    extends PropertyRepresentationConnector(servicesConfig, mock(classOf[HttpClient]))(ExecutionContext.global) {

  private var stubbedValidCodes: Seq[Long] = Nil
  private var stubbedAgentAuthResult: AgentAuthResult =
    AgentAuthResult(start = 15, total = 15, size = 15, filterTotal = 15, authorisations = Seq.empty[AgentAuthorisation])

  def stubAgentAuthResult(reps: AgentAuthResult) = stubbedAgentAuthResult = reps

  def getstubbedAgentAuthResult(): AgentAuthResult = stubbedAgentAuthResult

  def stubAgentCode(agentCode: Long) =
    stubbedValidCodes :+= agentCode

  def reset(): Unit =
    stubbedValidCodes = Nil

  override def revokeClientProperty(submissionId: String)(implicit hc: HeaderCarrier) = Future.successful(Unit)
}
