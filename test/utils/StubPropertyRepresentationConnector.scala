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

import connectors.PropertyRepresentationConnector
import controllers.Pagination
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubPropertyRepresentationConnector extends PropertyRepresentationConnector(StubHttp) {
  private var stubbedRepresentations: Seq[PropertyRepresentation] = Nil
  private var stubbedValidCodes: Seq[Long] = Nil

  def stubbedRepresentations(status: RepresentationStatus = RepresentationApproved): Seq[PropertyRepresentation] = stubbedRepresentations.filter(_.status == status)

  def stubRepresentation(rep: PropertyRepresentation) = stubbedRepresentations :+= rep

  def stubRepresentations(reps: Seq[PropertyRepresentation]) = stubbedRepresentations ++= reps

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

  override def forAgent(status: RepresentationStatus, agentOrganisationId: Int, pagination: Pagination)(implicit hc: HeaderCarrier) = Future.successful(
    PropertyRepresentations(totalPendingRequests = stubbedRepresentations.count(_.status == RepresentationPending),
      resultCount = Some(stubbedRepresentations.count(_.status == status)),
      propertyRepresentations = stubbedRepresentations.filter(_.status == status))
  )

  override def get(representationId: Long)(implicit hc: HeaderCarrier) = Future.successful(
    stubbedRepresentations.find(_.representationId == representationId)
  )

  override def find(linkId: Long)(implicit hc: HeaderCarrier) = Future.successful {
    stubbedRepresentations.filter(_.representationId == linkId)
  }

  override def create(reprRequest: RepresentationRequest)(implicit hc: HeaderCarrier) = Future.successful(Unit)


  override def revoke(permissionId: Long)(implicit hc: HeaderCarrier) = Future.successful(Unit)
}
