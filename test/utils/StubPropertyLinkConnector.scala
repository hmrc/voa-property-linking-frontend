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

import binders.propertylinks.GetPropertyLinksParameters
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PaginationParams
import models._
import models.propertylinking.payload.PropertyLinkPayload
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult, OwnerAuthorisation}
import org.mockito.Mockito.mock
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Configs._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StubPropertyLinkConnector extends PropertyLinkConnector(servicesConfig, mock(classOf[HttpClient])) {

  var stubbedLinks: Seq[PropertyLink] = Nil
  private var stubbedClientProperties: Seq[ClientProperty] = Nil
  private var stubbedOwnerAuthResult: OwnerAuthResult = OwnerAuthResult(start = 1, total = 15, size = 15, filterTotal = 15, authorisations = Seq.empty[OwnerAuthorisation])

  def getstubbedOwnerAuthResult() : OwnerAuthResult = stubbedOwnerAuthResult

  def stubOwnerAuthResult(reps: OwnerAuthResult) = { stubbedOwnerAuthResult = reps }

  override def createPropertyLink(propertyLinkPayload: PropertyLinkPayload)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    Future.successful(HttpResponse(200))

  override def getMyOrganisationsPropertyLinks(searchParams: GetPropertyLinksParameters,
                                    pagination: PaginationParams)
                                   (implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {

    Future.successful(stubbedOwnerAuthResult)
  }

  override def appointableProperties(organisationId: Long,
                                            pagination: AgentPropertiesParameters)
                                            (implicit hc: HeaderCarrier) = {
    Future.successful(stubbedOwnerAuthResult)
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
