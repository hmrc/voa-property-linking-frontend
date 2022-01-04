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

import binders.propertylinks.GetPropertyLinksParameters
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PaginationParams
import models._
import models.propertylinking.payload.PropertyLinkPayload
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import org.mockito.Mockito.mock
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Configs._

import scala.concurrent.{ExecutionContext, Future}

object StubPropertyLinkConnector
    extends PropertyLinkConnector(servicesConfig, mock(classOf[HttpClient]))(ExecutionContext.global)
    with HttpResponseUtils {

  var stubbedLinks: Seq[PropertyLink] = Nil
  private var stubbedClientPropertyLinks: Seq[ClientPropertyLink] = Nil
  private var stubbedOwnerAuthResult: OwnerAuthResult =
    OwnerAuthResult(start = 1, total = 15, size = 15, filterTotal = 15, authorisations = Seq.empty[OwnerAuthorisation])

  def getstubbedOwnerAuthResult(): OwnerAuthResult = stubbedOwnerAuthResult

  def stubOwnerAuthResult(reps: OwnerAuthResult) = stubbedOwnerAuthResult = reps

  override def createPropertyLink(propertyLinkPayload: PropertyLinkPayload)(
        implicit hc: HeaderCarrier): Future[HttpResponse] =
    Future.successful(emptyJsonHttpResponse(200))

  override def getMyOrganisationsPropertyLinks(searchParams: GetPropertyLinksParameters, pagination: PaginationParams)(
        implicit hc: HeaderCarrier): Future[OwnerAuthResult] =
    Future.successful(stubbedOwnerAuthResult)

  override def clientPropertyLink(submissionId: String)(
        implicit hc: HeaderCarrier): Future[Option[ClientPropertyLink]] = Future.successful {
    stubbedClientPropertyLinks.find(p => p.submissionId == submissionId)
  }

  def stubLink(link: PropertyLink) =
    stubbedLinks :+= link

  def stubLinks(links: Seq[PropertyLink]) =
    stubbedLinks ++= links

  def stubClientPropertyLink(clientProperty: ClientPropertyLink) =
    stubbedClientPropertyLinks :+= clientProperty

  def reset() = {
    stubbedLinks = Nil
    stubbedOwnerAuthResult = stubbedOwnerAuthResult.copy(authorisations = Seq.empty[OwnerAuthorisation])
  }
}
