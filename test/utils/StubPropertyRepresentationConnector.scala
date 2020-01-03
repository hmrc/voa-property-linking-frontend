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

import connectors.PropertyRepresentationConnector
import controllers.Pagination
import models._
import models.searchApi.{AgentAuthResult, AgentAuthorisation}
import org.mockito.Mockito.mock
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Configs._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StubPropertyRepresentationConnector extends PropertyRepresentationConnector(servicesConfig, mock(classOf[HttpClient])) {
  private var stubbedRepresentations: Seq[PropertyRepresentation] = Nil
  private var stubbedValidCodes: Seq[Long] = Nil
  private var stubbedAgentAuthResult: AgentAuthResult = AgentAuthResult(
    start = 15, total = 15, size= 15,
    filterTotal = 15,
    authorisations = Seq.empty[AgentAuthorisation])


  def stubbedRepresentations(status: RepresentationStatus = RepresentationApproved): Seq[PropertyRepresentation] = stubbedRepresentations.filter(_.status == status)

  def stubRepresentation(rep: PropertyRepresentation) = stubbedRepresentations :+= rep

  def stubRepresentations(reps: Seq[PropertyRepresentation]) = stubbedRepresentations ++= reps

  def stubAgentAuthResult(reps: AgentAuthResult) = { stubbedAgentAuthResult = reps }

  def getstubbedAgentAuthResult() : AgentAuthResult = stubbedAgentAuthResult


  def stubAgentCode(agentCode: Long) = {
    stubbedValidCodes :+= agentCode
  }

  def reset(): Unit = {
    stubbedRepresentations = Nil
    stubbedValidCodes = Nil
  }

  override def validateAgentCode(agentCode: Long, authorisationId: Long)(implicit hc: HeaderCarrier) = Future.successful {
    if(stubbedValidCodes.contains(agentCode)) { AgentCodeValidationResult(Some(123), None) } else { AgentCodeValidationResult(None, Some("INVALID_CODE")) }
  }

  override def forAgent(status: RepresentationStatus, agentOrganisationId: Long, pagination: Pagination)(implicit hc: HeaderCarrier) = Future.successful(
    PropertyRepresentations(totalPendingRequests = stubbedRepresentations.count(_.status == RepresentationPending),
      propertyRepresentations = stubbedRepresentations.filter(_.status == status))
  )

  override def create(reprRequest: RepresentationRequest)(implicit hc: HeaderCarrier) = Future.successful(Unit)

  override def response(representationResponse: RepresentationResponse)(implicit hc: HeaderCarrier) = Future.successful(Unit)

  override def revoke(permissionId: Long)(implicit hc: HeaderCarrier) = Future.successful(Unit)
}
