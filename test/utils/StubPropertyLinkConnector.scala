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

package utils

import connectors.fileUpload.FileMetadata
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{Pagination, PaginationSearchSort}
import models._
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import session.LinkingSessionRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object StubPropertyLinkConnector extends PropertyLinkConnector(StubServicesConfig, StubHttp) {

  var stubbedLinks: Seq[PropertyLink] = Nil
  private var stubbedClientProperties: Seq[ClientProperty] = Nil
  private var stubbedOwnerAuthResult: OwnerAuthResult = OwnerAuthResult(start = 15, total = 15, size= 15, filterTotal = 15, authorisations = Seq.empty[OwnerAuthorisation])

  def getstubbedOwnerAuthResult() : OwnerAuthResult = stubbedOwnerAuthResult

  def stubOwnerAuthResult(reps: OwnerAuthResult) = { stubbedOwnerAuthResult = reps }

  override def linkToProperty(data: FileMetadata)(implicit request: LinkingSessionRequest[_]): Future[Unit] = Future.successful(())

  override def linkedProperties(organisationId: Long, pagination: Pagination)(implicit hc: HeaderCarrier) = {
    Future.successful(PropertyLinkResponse(Some(stubbedLinks.size), stubbedLinks))
  }

  override def linkedPropertiesSearchAndSort(organisationId: Long,
                                            pagination: PaginationSearchSort,
                                             authorisationStatusFilter: Seq[PropertyLinkingStatus],
                                             representationStatusFilter: Seq[RepresentationStatus])
                                           (implicit hc: HeaderCarrier) = {
    Future.successful(stubbedOwnerAuthResult)
  }

  override def get(organisationId: Long, authorisationId: Long)(implicit hc: HeaderCarrier) = Future.successful {
    stubbedLinks.find(x => {x.authorisationId == authorisationId && x.organisationId == organisationId})
  }

  override def getLink(linkId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = Future.successful {
    stubbedLinks.find(_.authorisationId == linkId)
  }

  override def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Option[ClientProperty]] = Future.successful {
    stubbedClientProperties.find(p => p.authorisationId == authorisationId && p.ownerOrganisationId == clientOrgId)
  }

  def stubLink(link: PropertyLink) = {
    stubbedLinks :+= link
  }

  def stubLinks(links: Seq[PropertyLink]) = {
    stubbedLinks ++= links
  }

  def stubClientProperty(clientProperty: ClientProperty) = {
    stubbedClientProperties :+= clientProperty
  }

  def reset() = {
    stubbedLinks = Nil
    stubbedClientProperties = Nil
    stubbedOwnerAuthResult = stubbedOwnerAuthResult.copy(authorisations = Seq.empty[OwnerAuthorisation])
  }
}
